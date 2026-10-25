package OTO.calculo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.calculo.ResultadoDtos.CampanhaPublicadaResponse;
import OTO.calculo.ResultadoDtos.FaixaQuantidade;
import OTO.calculo.ResultadoDtos.FaixaRef;
import OTO.calculo.ResultadoDtos.PreviaResponse;
import OTO.calculo.ResultadoDtos.ResultadoResponse;
import OTO.calculo.ResultadoDtos.ResultadoTipoResponse;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.Estabelecimento;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaRepository;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.RespostaOcupacao;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.RodadaPesquisa;
import OTO.pesquisa.TipoPeriodo;
import OTO.referencia.FaixaOcupacao;
import OTO.referencia.FaixaOcupacaoRepository;
import OTO.referencia.TipoHospedagem;
import OTO.referencia.TipoHospedagemRepository;

/**
 * cálculo, prévia e publicação dos resultados. Na publicação o resultado é gravado e
 * fica congelado: mudanças posteriores no inventário não alteram boletins antigos.
 */
@Service
public class ResultadoService {

    /** recortes por tipo com menos respondentes que isto ficam ocultos para quem não é ADMIN. */
    public static final int MINIMO_RESPONDENTES_POR_TIPO = 3;

    private final CampanhaRepository campanhas;
    private final BaseCalculoService bases;
    private final ResultadoCampanhaRepository resultados;
    private final ResultadoCampanhaTipoRepository resultadosTipo;
    private final ResultadoCampanhaFaixaRepository resultadosFaixa;
    private final FaixaOcupacaoRepository faixas;
    private final TipoHospedagemRepository tipos;
    private final AuditoriaService auditoria;

    public ResultadoService(CampanhaRepository campanhas, BaseCalculoService bases,
            ResultadoCampanhaRepository resultados, ResultadoCampanhaTipoRepository resultadosTipo,
            ResultadoCampanhaFaixaRepository resultadosFaixa, FaixaOcupacaoRepository faixas,
            TipoHospedagemRepository tipos, AuditoriaService auditoria) {
        this.campanhas = campanhas;
        this.bases = bases;
        this.resultados = resultados;
        this.resultadosTipo = resultadosTipo;
        this.resultadosFaixa = resultadosFaixa;
        this.faixas = faixas;
        this.tipos = tipos;
        this.auditoria = auditoria;
    }

    /** resultado calculado ainda não gravado (as entidades só vão para o banco na publicação). */
    private record Calculo(ResultadoCampanha resultado, List<ResultadoCampanhaTipo> porTipo,
            List<ResultadoCampanhaFaixa> faixas) {
    }

    /** simula o resultado com as respostas atuais. Pode ser chamada com a pesquisa ainda aberta. */
    @Transactional(readOnly = true)
    public PreviaResponse previa(Long campanhaId) {
        Campanha campanha = carregar(campanhaId);
        BaseCalculo base = bases.montar(campanha);
        List<String> impedimentos = new ArrayList<>();
        if (campanha.isPublicada()) {
            impedimentos.add("A campanha já foi publicada.");
        }
        if (!campanha.getRodada().isEncerrada()) {
            impedimentos.add("A pesquisa " + campanha.getRodada().getCodigo() + " precisa ser encerrada antes da publicação.");
        }
        BigDecimal percentual = Numeros.duasCasas(base.percentualLeitosRespondentes());
        BigDecimal minimo = base.parametro().getPercentualMinimoLeitosPublicacao();
        if (percentual.compareTo(minimo) < 0) {
            impedimentos.add("Leitos respondentes: " + Numeros.emPortugues(percentual) + "% (mínimo para publicar: "
                    + Numeros.emPortugues(minimo) + "%).");
        }
        ResultadoResponse resultado = null;
        if (base.qtdRespondentes() == 0) {
            impedimentos.add("Nenhum meio de hospedagem respondeu.");
        }
        else {
            Calculo calculo = calcular(base);
            resultado = montar(campanha, calculo.resultado(), calculo.porTipo(), calculo.faixas(), true);
        }
        return new PreviaResponse(campanhaId, base.participantes().size(), base.qtdRespondentes(), base.leitosTotal(),
                base.leitosRespondentes(), percentual, minimo, impedimentos.isEmpty(), List.copyOf(impedimentos),
                avisos(base), resultado);
    }

    @Transactional
    public ResultadoResponse publicar(Long campanhaId, UsuarioLogado admin) {
        Campanha campanha = carregar(campanhaId);
        if (campanha.isPublicada()) {
            throw new RegraNegocioException("A campanha já foi publicada.");
        }
        RodadaPesquisa rodada = campanha.getRodada();
        if (!rodada.isEncerrada()) {
            throw new RegraNegocioException("Encerre a pesquisa " + rodada.getCodigo() + " antes de publicar.");
        }
        BaseCalculo base = bases.montar(campanha);
        if (base.qtdRespondentes() == 0) {
            throw new RegraNegocioException("Nenhum meio de hospedagem respondeu à campanha.");
        }
        Calculo calculo = calcular(base);
        BigDecimal percentual = calculo.resultado().getPercentualLeitosRespondentes();
        BigDecimal minimo = base.parametro().getPercentualMinimoLeitosPublicacao();
        if (percentual.compareTo(minimo) < 0) {
            throw new RegraNegocioException("Leitos respondentes: " + Numeros.emPortugues(percentual)
                    + "%. O mínimo para publicar é " + Numeros.emPortugues(minimo) + "%.");
        }
        ResultadoCampanha salvo = resultados.save(calculo.resultado());
        resultadosTipo.saveAll(calculo.porTipo());
        resultadosFaixa.saveAll(calculo.faixas());
        campanha.publicar(admin.id());
        auditoria.registrar(admin.id(), "campanha", campanhaId, "PUBLICAR", null, Map.of(
                "taxaOcupacao", salvo.getTaxaOcupacao(),
                "fluxoTotal", salvo.getFluxoTotal(),
                "percentualLeitosRespondentes", salvo.getPercentualLeitosRespondentes(),
                "parametroCalculoId", salvo.getParametroCalculoId()));
        return montar(campanha, salvo, calculo.porTipo(), calculo.faixas(), true);
    }

    /** desfaz uma publicação errada. Depois disso a pesquisa pode ser reaberta para correções. */
    @Transactional
    public void cancelarPublicacao(Long campanhaId, UsuarioLogado admin) {
        Campanha campanha = carregar(campanhaId);
        if (!campanha.isPublicada()) {
            throw new RegraNegocioException("A campanha não está publicada.");
        }
        resultadosFaixa.deleteByIdCampanhaId(campanhaId);
        resultadosTipo.deleteByIdCampanhaId(campanhaId);
        resultados.deleteById(campanhaId);
        campanha.cancelarPublicacao();
        auditoria.registrar(admin.id(), "campanha", campanhaId, "CANCELAR_PUBLICACAO", null, null);
    }

    /** resultado publicado. Para quem não é ADMIN, tipos com poucos respondentes vêm sem valores. */
    @Transactional(readOnly = true)
    public ResultadoResponse resultado(Long campanhaId, UsuarioLogado usuario) {
        Campanha campanha = carregar(campanhaId);
        ResultadoCampanha resultado = resultados.findById(campanhaId)
                .filter(r -> campanha.isPublicada())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "A campanha " + campanhaId + " ainda não tem resultado publicado."));
        return montar(campanha, resultado, resultadosTipo.findByIdCampanhaId(campanhaId),
                resultadosFaixa.findByIdCampanhaId(campanhaId), usuario.isAdmin());
    }

    @Transactional(readOnly = true)
    public byte[] csv(Long campanhaId, UsuarioLogado usuario) {
        return ResultadoCsv.gerar(resultado(campanhaId, usuario));
    }

    @Transactional(readOnly = true)
    public List<CampanhaPublicadaResponse> publicadas(Modalidade modalidade, TipoPeriodo tipoPeriodo, Integer ano) {
        List<Campanha> lista = campanhas.findPublicadas().stream()
                .filter(c -> modalidade == null || c.getModalidade() == modalidade)
                .filter(c -> tipoPeriodo == null || c.getTipoPeriodo() == tipoPeriodo)
                .filter(c -> ano == null || c.getPeriodoInicio().getYear() == ano)
                .toList();
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, ResultadoCampanha> porCampanha = new HashMap<>();
        resultados.findByCampanhaIdIn(lista.stream().map(Campanha::getId).toList())
                .forEach(r -> porCampanha.put(r.getCampanhaId(), r));
        List<FaixaOcupacao> listaFaixas = faixas.findAllByOrderByPercentualMinimo();
        return lista.stream()
                .filter(c -> porCampanha.containsKey(c.getId()))
                .map(c -> {
                    ResultadoCampanha r = porCampanha.get(c.getId());
                    return new CampanhaPublicadaResponse(c.getId(), c.getNome(), c.getModalidade(), c.getTipoPeriodo(),
                            c.getFeriadoCodigo(), c.getPeriodoInicio(), c.getPeriodoFim(), c.getPublicadaEm(),
                            r.getTaxaOcupacao(), faixaDe(r.getTaxaOcupacao(), listaFaixas), r.getFluxoTotal(),
                            r.getDiariaMedia());
                })
                .toList();
    }

    /** faixa de maior percentual mínimo que não passa da taxa. */
    public static FaixaRef faixaDe(BigDecimal taxa, List<FaixaOcupacao> lista) {
        if (taxa == null) {
            return null;
        }
        FaixaOcupacao escolhida = null;
        for (FaixaOcupacao f : lista) {
            if (taxa.compareTo(f.getPercentualMinimo()) >= 0
                    && (escolhida == null || f.getPercentualMinimo().compareTo(escolhida.getPercentualMinimo()) > 0)) {
                escolhida = f;
            }
        }
        return escolhida == null ? null : new FaixaRef(escolhida.getCodigo(), escolhida.getNome());
    }

    private Calculo calcular(BaseCalculo base) {
        CalculadoraOcupacao.Entrada entrada = base.paraEntrada();
        CalculadoraOcupacao.Resultado r;
        try {
            r = CalculadoraOcupacao.calcular(entrada);
        }
        catch (IllegalArgumentException e) {
            throw new RegraNegocioException(e.getMessage());
        }
        Long campanhaId = base.campanha().getId();
        List<CalculadoraOcupacao.Faixa> listaFaixas = faixas.findAllByOrderByPercentualMinimo().stream()
                .map(f -> new CalculadoraOcupacao.Faixa(f.getCodigo(), f.getPercentualMinimo().doubleValue()))
                .toList();
        List<ResultadoCampanhaFaixa> distribuicao = new ArrayList<>();
        CalculadoraOcupacao.distribuirPorFaixa(entrada.meios(), listaFaixas)
                .forEach((codigo, qtd) -> distribuicao.add(new ResultadoCampanhaFaixa(campanhaId, codigo, qtd)));
        return new Calculo(
                ResultadoCampanha.de(campanhaId, base.parametro().getId(), r),
                r.porTipo().stream().map(t -> ResultadoCampanhaTipo.de(campanhaId, t)).toList(),
                List.copyOf(distribuicao));
    }

    private ResultadoResponse montar(Campanha campanha, ResultadoCampanha r, List<ResultadoCampanhaTipo> porTipo,
            List<ResultadoCampanhaFaixa> distribuicao, boolean completo) {
        Map<String, TipoHospedagem> tiposPorCodigo = new HashMap<>();
        tipos.findAll().forEach(t -> tiposPorCodigo.put(t.getCodigo(), t));
        List<ResultadoTipoResponse> listaTipos = porTipo.stream()
                .sorted(Comparator.comparingInt(t -> ordem(tiposPorCodigo.get(t.getTipoHospedagemCodigo()))))
                .map(t -> {
                    boolean omitido = !completo && t.getQtdMhRespondentes() < MINIMO_RESPONDENTES_POR_TIPO;
                    TipoHospedagem tipo = tiposPorCodigo.get(t.getTipoHospedagemCodigo());
                    return new ResultadoTipoResponse(t.getTipoHospedagemCodigo(),
                            tipo == null ? t.getTipoHospedagemCodigo() : tipo.getNome(),
                            t.getQtdMh(), t.getQtdMhRespondentes(), t.getQtdUh(),
                            omitido ? null : t.getTaxaOcupacao(),
                            omitido ? null : t.getDiariaMedia(),
                            omitido ? null : t.getRevpar(),
                            omitido ? null : t.getReceitaEstimada(),
                            omitido);
                })
                .toList();

        List<FaixaOcupacao> listaFaixas = faixas.findAllByOrderByPercentualMinimo();
        Map<String, Integer> qtdPorFaixa = new HashMap<>();
        distribuicao.forEach(f -> qtdPorFaixa.put(f.getFaixaCodigo(), f.getQtdMh()));
        int respondentes = r.getQtdMhRespondentes();
        List<FaixaQuantidade> listaDistribuicao = listaFaixas.stream()
                .sorted(Comparator.comparing(FaixaOcupacao::getPercentualMinimo).reversed())
                .map(f -> {
                    int qtd = qtdPorFaixa.getOrDefault(f.getCodigo(), 0);
                    return new FaixaQuantidade(f.getCodigo(), f.getNome(), f.getPercentualMinimo(), qtd,
                            Numeros.duasCasas(respondentes == 0 ? 0 : 100.0 * qtd / respondentes));
                })
                .toList();

        BigDecimal percentualMh = Numeros.duasCasas(r.getQtdMh() == 0 ? 0 : 100.0 * respondentes / r.getQtdMh());
        return new ResultadoResponse(CampanhaResponse.de(campanha), campanha.isPublicada(), r.getTaxaOcupacao(),
                faixaDe(r.getTaxaOcupacao(), listaFaixas), r.getQtdMh(), respondentes, percentualMh,
                r.getLeitosTotal(), r.getLeitosRespondentes(), r.getPercentualLeitosRespondentes(), r.getFluxoMh(),
                r.getFluxoRhc(), r.getFluxoDayUse(), r.getFluxoTotal(), r.getFluxoMedioDiario(), r.getDiariaMedia(),
                r.getReceitaHospedagem(), r.getReceitaSemHospedagem(), r.getParametroCalculoId(), r.getCalculadoEm(),
                listaTipos, listaDistribuicao);
    }

    /** situações que não impedem a publicação, mas que os responsáveis do OTO devem conferir antes. */
    private static List<String> avisos(BaseCalculo base) {
        List<String> avisos = new ArrayList<>();
        Set<Long> semCapacidade = new HashSet<>();
        for (Estabelecimento e : base.semCapacidade()) {
            semCapacidade.add(e.getId());
            avisos.add(e.getNomeFantasia() + ": sem UH/leitos cadastrados para " + base.dataReferencia()
                    + "; fica fora do cálculo.");
        }
        for (RespostaOcupacao r : base.respostasDesconsideradas()) {
            if (!semCapacidade.contains(r.getEstabelecimento().getId())) {
                avisos.add(r.getEstabelecimento().getNomeFantasia()
                        + ": respondeu, mas está inativo ou irregular; a resposta não entra no cálculo.");
            }
        }
        if (base.rhc() == null) {
            avisos.add("Não há totais de hospedagem caseira (RHC) cadastrados: o fluxo do RHC fica zerado.");
        }
        base.diariasInformadas().stream()
                .sorted(Comparator.comparing(d -> d.isGeral() ? "" : d.getTipoHospedagemCodigo()))
                .forEach(d -> avisos.add((d.isGeral() ? "Diária média geral" : "Diária de " + d.getTipoHospedagemCodigo())
                        + " digitada pela equipe (R$ " + Numeros.emPortugues(d.getDiariaMedia())
                        + "): substitui a média das respostas."));
        Map<String, Integer> respondentesPorTipo = new HashMap<>();
        for (BaseCalculo.Participante p : base.participantes()) {
            if (p.respondeu()) {
                respondentesPorTipo.merge(p.estabelecimento().getTipoHospedagemCodigo(), 1, Integer::sum);
            }
        }
        respondentesPorTipo.forEach((tipo, qtd) -> {
            if (qtd < MINIMO_RESPONDENTES_POR_TIPO) {
                avisos.add("Tipo " + tipo + " com " + qtd + " respondente(s): na divulgação, os valores do tipo ficam ocultos.");
            }
        });
        return List.copyOf(avisos);
    }

    private static int ordem(TipoHospedagem tipo) {
        return tipo == null || tipo.getOrdem() == null ? Integer.MAX_VALUE : tipo.getOrdem();
    }

    private Campanha carregar(Long id) {
        return campanhas.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Campanha", id));
    }
}
