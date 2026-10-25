package OTO.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.calculo.ResultadoCampanha;
import OTO.calculo.ResultadoCampanhaRepository;
import OTO.calculo.ResultadoService;
import OTO.comum.excecao.RegraNegocioException;
import OTO.dashboard.DashboardDtos.AcumuladoAno;
import OTO.dashboard.DashboardDtos.Comparativo;
import OTO.dashboard.DashboardDtos.FeriadoSerie;
import OTO.dashboard.DashboardDtos.Indicadores;
import OTO.dashboard.DashboardDtos.PontoAnual;
import OTO.dashboard.DashboardDtos.PontoMensal;
import OTO.dashboard.DashboardDtos.ResumoResponse;
import OTO.dashboard.DashboardDtos.SerieAno;
import OTO.dashboard.DashboardDtos.SerieMensalResponse;
import OTO.historico.Indicador;
import OTO.historico.IndicadorHistorico;
import OTO.historico.IndicadorHistoricoRepository;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaRepository;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.TipoPeriodo;
import OTO.referencia.FaixaOcupacao;
import OTO.referencia.FaixaOcupacaoRepository;
import OTO.referencia.FeriadoEvento;
import OTO.referencia.FeriadoEventoRepository;

/**
 * indicadores gerais da cidade para os dashboards. Junta os resultados publicados no
 * sistema com a série histórica; quando os dois têm o mesmo período, vale o do sistema.
 */
@Service
public class DashboardService {

    private static final int MAXIMO_ANOS = 20;

    private final CampanhaRepository campanhas;
    private final ResultadoCampanhaRepository resultados;
    private final IndicadorHistoricoRepository historico;
    private final FaixaOcupacaoRepository faixas;
    private final FeriadoEventoRepository feriados;

    public DashboardService(CampanhaRepository campanhas, ResultadoCampanhaRepository resultados,
            IndicadorHistoricoRepository historico, FaixaOcupacaoRepository faixas, FeriadoEventoRepository feriados) {
        this.campanhas = campanhas;
        this.resultados = resultados;
        this.historico = historico;
        this.faixas = faixas;
        this.feriados = feriados;
    }

    private enum Origem { SISTEMA, HISTORICO }

    /** Mês (mes preenchido) ou feriado (feriado preenchido) de um ano. */
    private record Periodo(int ano, Integer mes, String feriado) {
    }

    private record Valor(BigDecimal numero, Origem origem) {
    }

    /** Valores gerais de um período, cada um com a sua origem. */
    private record Valores(Valor taxa, Valor fluxo, Valor diaria) {

        static final Valores VAZIO = new Valores(null, null, null);

        Valor de(Indicador indicador) {
            return switch (indicador) {
                case TAXA_OCUPACAO -> taxa;
                case FLUXO_TURISTAS -> fluxo;
                case DIARIA_MEDIA -> diaria;
            };
        }

        Valores com(Indicador indicador, Valor valor) {
            return switch (indicador) {
                case TAXA_OCUPACAO -> new Valores(valor, fluxo, diaria);
                case FLUXO_TURISTAS -> new Valores(taxa, valor, diaria);
                case DIARIA_MEDIA -> new Valores(taxa, fluxo, valor);
            };
        }

        boolean vazio() {
            return taxa == null && fluxo == null && diaria == null;
        }

        String origem() {
            for (Valor v : new Valor[] { taxa, fluxo, diaria }) {
                if (v != null && v.origem() == Origem.SISTEMA) {
                    return Origem.SISTEMA.name();
                }
            }
            return vazio() ? null : Origem.HISTORICO.name();
        }
    }

    private record Publicado(Campanha campanha, ResultadoCampanha resultado) {
    }

    @Transactional(readOnly = true)
    public ResumoResponse resumo(Modalidade modalidade) {
        List<Publicado> publicados = publicados(modalidade);
        Map<Periodo, Valores> indice = indice(modalidade, publicados);
        List<FaixaOcupacao> listaFaixas = faixas.findAllByOrderByPercentualMinimo();
        Publicado ultimoMes = publicados.stream()
                .filter(p -> p.campanha().getTipoPeriodo() == TipoPeriodo.MENSAL).findFirst().orElse(null);
        Publicado ultimoFeriado = publicados.stream()
                .filter(p -> p.campanha().getTipoPeriodo() == TipoPeriodo.FERIADO).findFirst().orElse(null);
        return new ResumoResponse(modalidade,
                ultimoMes == null ? null : indicadores(ultimoMes, indice, listaFaixas),
                ultimoFeriado == null ? null : indicadores(ultimoFeriado, indice, listaFaixas),
                ultimoMes == null ? null : acumulado(ultimoMes.campanha().getPeriodoInicio().getYear(), indice));
    }

    @Transactional(readOnly = true)
    public SerieMensalResponse serieMensal(Indicador indicador, Modalidade modalidade, int anoInicio, int anoFim) {
        validarAnos(anoInicio, anoFim);
        Map<Periodo, Valores> indice = indice(modalidade, publicados(modalidade));
        List<SerieAno> anos = new ArrayList<>();
        for (int ano = anoInicio; ano <= anoFim; ano++) {
            List<PontoMensal> meses = new ArrayList<>();
            for (int mes = 1; mes <= 12; mes++) {
                Valor valor = indice.getOrDefault(new Periodo(ano, mes, null), Valores.VAZIO).de(indicador);
                meses.add(new PontoMensal(mes, valor == null ? null : valor.numero(),
                        valor == null ? null : valor.origem().name()));
            }
            anos.add(new SerieAno(ano, List.copyOf(meses)));
        }
        return new SerieMensalResponse(indicador, modalidade, List.copyOf(anos));
    }

    /** Feriados ano a ano. Sem feriado escolhido, lista só os que têm algum dado no intervalo. */
    @Transactional(readOnly = true)
    public List<FeriadoSerie> feriados(Modalidade modalidade, String feriadoCodigo, int anoInicio, int anoFim) {
        validarAnos(anoInicio, anoFim);
        Map<Periodo, Valores> indice = indice(modalidade, publicados(modalidade));
        String filtro = feriadoCodigo == null || feriadoCodigo.isBlank() ? null : feriadoCodigo.trim();
        List<FeriadoSerie> series = new ArrayList<>();
        for (FeriadoEvento f : feriados.findAllByOrderByOrdem()) {
            if (filtro != null && !filtro.equals(f.getCodigo())) {
                continue;
            }
            List<PontoAnual> pontos = new ArrayList<>();
            boolean temDado = false;
            for (int ano = anoInicio; ano <= anoFim; ano++) {
                Valores v = indice.getOrDefault(new Periodo(ano, null, f.getCodigo()), Valores.VAZIO);
                temDado |= !v.vazio();
                pontos.add(new PontoAnual(ano, numero(v.taxa()), numero(v.fluxo()), numero(v.diaria()), v.origem()));
            }
            if (temDado || filtro != null) {
                series.add(new FeriadoSerie(f.getCodigo(), f.getNome(), List.copyOf(pontos)));
            }
        }
        return series;
    }

    private Indicadores indicadores(Publicado publicado, Map<Periodo, Valores> indice, List<FaixaOcupacao> listaFaixas) {
        Campanha c = publicado.campanha();
        ResultadoCampanha r = publicado.resultado();
        Periodo atual = periodoDe(c);
        Periodo anterior = new Periodo(atual.ano() - 1, atual.mes(), atual.feriado());
        Valores v = indice.getOrDefault(anterior, Valores.VAZIO);
        Comparativo comparativo = v.vazio() ? null : new Comparativo(anterior.ano(),
                numero(v.taxa()), numero(v.fluxo()), numero(v.diaria()),
                diferenca(r.getTaxaOcupacao(), numero(v.taxa())),
                variacao(r.getFluxoTotal(), numero(v.fluxo())),
                variacao(r.getDiariaMedia(), numero(v.diaria())),
                v.origem());
        return new Indicadores(c.getId(), c.getNome(), c.getTipoPeriodo(), c.getFeriadoCodigo(), c.getPeriodoInicio(),
                c.getPeriodoFim(), r.getTaxaOcupacao(), ResultadoService.faixaDe(r.getTaxaOcupacao(), listaFaixas),
                r.getFluxoTotal(), r.getFluxoMedioDiario(), r.getDiariaMedia(), r.getReceitaHospedagem(),
                r.getReceitaSemHospedagem(), r.getPercentualLeitosRespondentes(), comparativo);
    }

    private static AcumuladoAno acumulado(int ano, Map<Periodo, Valores> indice) {
        BigDecimal somaTaxas = BigDecimal.ZERO;
        BigDecimal somaFluxos = BigDecimal.ZERO;
        int mesesComTaxa = 0;
        int mesesComFluxo = 0;
        for (int mes = 1; mes <= 12; mes++) {
            Valores v = indice.getOrDefault(new Periodo(ano, mes, null), Valores.VAZIO);
            if (v.taxa() != null) {
                somaTaxas = somaTaxas.add(v.taxa().numero());
                mesesComTaxa++;
            }
            if (v.fluxo() != null) {
                somaFluxos = somaFluxos.add(v.fluxo().numero());
                mesesComFluxo++;
            }
        }
        BigDecimal media = mesesComTaxa == 0 ? null
                : somaTaxas.divide(BigDecimal.valueOf(mesesComTaxa), 2, RoundingMode.HALF_UP);
        return new AcumuladoAno(ano, mesesComTaxa, media, mesesComFluxo, mesesComFluxo == 0 ? null : somaFluxos);
    }

    /** Campanhas publicadas da modalidade com o resultado, da mais recente para a mais antiga. */
    private List<Publicado> publicados(Modalidade modalidade) {
        List<Campanha> lista = campanhas.findPublicadas().stream()
                .filter(c -> c.getModalidade() == modalidade)
                .toList();
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, ResultadoCampanha> porCampanha = new HashMap<>();
        resultados.findByCampanhaIdIn(lista.stream().map(Campanha::getId).toList())
                .forEach(r -> porCampanha.put(r.getCampanhaId(), r));
        return lista.stream()
                .filter(c -> porCampanha.containsKey(c.getId()))
                .map(c -> new Publicado(c, porCampanha.get(c.getId())))
                .toList();
    }

    /** Valores gerais por período: primeiro a série histórica, depois os resultados do sistema por cima. */
    private Map<Periodo, Valores> indice(Modalidade modalidade, List<Publicado> publicados) {
        Map<Periodo, Valores> indice = new HashMap<>();
        for (IndicadorHistorico h : historico.findAll()) {
            if (h.getModalidade() != modalidade || h.getTipoHospedagemCodigo() != null) {
                continue;
            }
            Periodo p = new Periodo(h.getAno(), h.getMes(), h.getFeriadoCodigo());
            indice.put(p, indice.getOrDefault(p, Valores.VAZIO)
                    .com(h.getIndicador(), new Valor(h.getValor(), Origem.HISTORICO)));
        }
        for (Publicado publicado : publicados) {
            Periodo p = periodoDe(publicado.campanha());
            ResultadoCampanha r = publicado.resultado();
            Valores v = indice.getOrDefault(p, Valores.VAZIO)
                    .com(Indicador.TAXA_OCUPACAO, new Valor(r.getTaxaOcupacao(), Origem.SISTEMA))
                    .com(Indicador.FLUXO_TURISTAS, new Valor(r.getFluxoTotal(), Origem.SISTEMA));
            if (r.getDiariaMedia() != null) {
                v = v.com(Indicador.DIARIA_MEDIA, new Valor(r.getDiariaMedia(), Origem.SISTEMA));
            }
            indice.put(p, v);
        }
        return indice;
    }

    private static Periodo periodoDe(Campanha c) {
        int ano = c.getPeriodoInicio().getYear();
        return c.getTipoPeriodo() == TipoPeriodo.MENSAL
                ? new Periodo(ano, c.getPeriodoInicio().getMonthValue(), null)
                : new Periodo(ano, null, c.getFeriadoCodigo());
    }

    private static BigDecimal numero(Valor valor) {
        return valor == null ? null : valor.numero();
    }

    private static BigDecimal diferenca(BigDecimal atual, BigDecimal anterior) {
        return atual == null || anterior == null ? null : atual.subtract(anterior).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (atual == null || anterior == null || anterior.signum() == 0) {
            return null;
        }
        return atual.subtract(anterior).multiply(BigDecimal.valueOf(100)).divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private static void validarAnos(int anoInicio, int anoFim) {
        if (anoInicio > anoFim) {
            throw new RegraNegocioException("O ano inicial não pode ser maior que o final.");
        }
        if (anoFim - anoInicio + 1 > MAXIMO_ANOS) {
            throw new RegraNegocioException("Escolha no máximo " + MAXIMO_ANOS + " anos.");
        }
        if (anoInicio < 2000 || anoFim > 2100) {
            throw new RegraNegocioException("Anos válidos: de 2000 a 2100.");
        }
    }
}
