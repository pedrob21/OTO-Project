package OTO.historico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.historico.HistoricoDtos.ErroLinha;
import OTO.historico.HistoricoDtos.HistoricoRequest;
import OTO.historico.HistoricoDtos.HistoricoResponse;
import OTO.historico.HistoricoDtos.ImportacaoResponse;
import OTO.historico.LeitorCsvHistorico.FalhaCsv;
import OTO.historico.LeitorCsvHistorico.Leitura;
import OTO.historico.LeitorCsvHistorico.LinhaCsv;
import OTO.pesquisa.Modalidade;
import OTO.referencia.FeriadoEvento;
import OTO.referencia.FeriadoEventoRepository;
import OTO.referencia.TipoHospedagem;
import OTO.referencia.TipoHospedagemRepository;

/**
 * Série histórica de 2017 em diante, para os comparativos ano a ano dos dashboards.
 * A combinação indicador + modalidade + ano + mês/feriado + tipo é única: gravar de
 * novo a mesma combinação substitui o valor.
 */
@Service
public class HistoricoService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final int MAXIMO_ERROS_LISTADOS = 100;

    private final IndicadorHistoricoRepository historico;
    private final FeriadoEventoRepository feriados;
    private final TipoHospedagemRepository tipos;
    private final AuditoriaService auditoria;

    public HistoricoService(IndicadorHistoricoRepository historico, FeriadoEventoRepository feriados,
            TipoHospedagemRepository tipos, AuditoriaService auditoria) {
        this.historico = historico;
        this.feriados = feriados;
        this.tipos = tipos;
        this.auditoria = auditoria;
    }

    /** Um valor a gravar, já normalizado. */
    private record Linha(Indicador indicador, Modalidade modalidade, int ano, Integer mes, String feriadoCodigo,
            String tipoHospedagemCodigo, BigDecimal valor, String observacao) {

        String chave() {
            return HistoricoService.chave(indicador, modalidade, ano, mes, feriadoCodigo, tipoHospedagemCodigo);
        }
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> listar(Indicador indicador, Modalidade modalidade, Integer ano, String tipo) {
        String tipoFiltro = tipo == null || tipo.isBlank() ? null : tipo.trim();
        return historico.findAll(Sort.by("ano", "mes", "feriadoCodigo", "indicador")).stream()
                .filter(h -> indicador == null || h.getIndicador() == indicador)
                .filter(h -> modalidade == null || h.getModalidade() == modalidade)
                .filter(h -> ano == null || h.getAno() == ano)
                .filter(h -> tipoFiltro == null || tipoFiltro.equals(h.getTipoHospedagemCodigo()))
                .map(HistoricoResponse::de)
                .toList();
    }

    @Transactional
    public HistoricoResponse salvar(HistoricoRequest r, UsuarioLogado admin) {
        Linha linha = new Linha(r.indicador(), r.modalidade(), r.ano(), r.mes(), texto(r.feriadoCodigo()),
                texto(r.tipoHospedagemCodigo()), r.valor(), texto(r.observacao()));
        List<String> erros = validar(linha, codigosDeFeriado(), codigosDeTipo());
        if (!erros.isEmpty()) {
            throw new RegraNegocioException(primeiraMaiuscula(String.join("; ", erros)) + ".");
        }
        Map<String, IndicadorHistorico> existentes = existentes();
        IndicadorHistorico gravado = gravar(linha, r.fonte().trim(), existentes);
        auditoria.registrar(admin.id(), "indicador_historico", gravado.getId(), "GRAVAR", null, r);
        return HistoricoResponse.de(gravado);
    }

    /**
     * Corrige um valor já gravado, inclusive o indicador, o período ou o tipo. Se a nova
     * combinação já tiver outro valor, nada muda: corrija aquele ou exclua-o antes.
     */
    @Transactional
    public HistoricoResponse alterar(Long id, HistoricoRequest r, UsuarioLogado admin) {
        IndicadorHistorico h = historico.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Valor histórico", id));
        Linha linha = new Linha(r.indicador(), r.modalidade(), r.ano(), r.mes(), texto(r.feriadoCodigo()),
                texto(r.tipoHospedagemCodigo()), r.valor(), texto(r.observacao()));
        List<String> erros = validar(linha, codigosDeFeriado(), codigosDeTipo());
        if (!erros.isEmpty()) {
            throw new RegraNegocioException(primeiraMaiuscula(String.join("; ", erros)) + ".");
        }
        IndicadorHistorico outro = existentes().get(linha.chave());
        if (outro != null && !outro.getId().equals(id)) {
            throw new RegraNegocioException("Já existe outro valor para este indicador, período e tipo (id "
                    + outro.getId() + "). Corrija aquele ou exclua-o antes.");
        }
        HistoricoResponse antes = HistoricoResponse.de(h);
        h.corrigirCombinacao(linha.indicador(), linha.modalidade(), linha.ano(), linha.mes(), linha.feriadoCodigo(),
                linha.tipoHospedagemCodigo());
        h.definirValor(linha.valor(), linha.observacao(), r.fonte().trim());
        HistoricoResponse depois = HistoricoResponse.de(h);
        auditoria.registrar(admin.id(), "indicador_historico", id, "ATUALIZAR", antes, depois);
        return depois;
    }

    @Transactional
    public void excluir(Long id, UsuarioLogado admin) {
        IndicadorHistorico h = historico.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Valor histórico", id));
        historico.delete(h);
        auditoria.registrar(admin.id(), "indicador_historico", id, "EXCLUIR", HistoricoResponse.de(h), null);
    }

    /** Importa o CSV inteiro ou nada: com qualquer erro, devolve a lista de erros e não grava. */
    @Transactional
    public ImportacaoResponse importar(byte[] conteudo, String fonte, UsuarioLogado admin) {
        if (fonte == null || fonte.isBlank() || fonte.trim().length() > 200) {
            throw new RegraNegocioException("Informe a fonte dos dados (até 200 caracteres), ex.: planilha Indicadores do Turismo 2017-2025.");
        }
        Leitura leitura = LeitorCsvHistorico.ler(conteudo);
        List<ErroLinha> erros = new ArrayList<>();
        for (FalhaCsv falha : leitura.falhas()) {
            erros.add(new ErroLinha(falha.numero(), falha.mensagem()));
        }
        Set<String> feriadosValidos = codigosDeFeriado();
        Set<String> tiposValidos = codigosDeTipo();
        Set<String> chaves = new HashSet<>();
        List<Linha> validas = new ArrayList<>();
        for (LinhaCsv l : leitura.linhas()) {
            Linha linha = new Linha(l.indicador(), l.modalidade(), l.ano(), l.mes(), l.feriadoCodigo(),
                    l.tipoHospedagemCodigo(), l.valor().setScale(2, RoundingMode.HALF_UP), l.observacao());
            List<String> problemas = validar(linha, feriadosValidos, tiposValidos);
            if (problemas.isEmpty() && !chaves.add(linha.chave())) {
                problemas = List.of("mesma combinação de indicador, período e tipo repetida no arquivo");
            }
            if (problemas.isEmpty()) {
                validas.add(linha);
            }
            else {
                erros.add(new ErroLinha(l.numero(), String.join("; ", problemas)));
            }
        }
        int lidas = leitura.linhas().size() + leitura.falhas().size();
        if (lidas == 0) {
            erros.add(new ErroLinha(0, "o arquivo não tem linhas de dados"));
        }
        if (!erros.isEmpty()) {
            erros.sort((a, b) -> Integer.compare(a.linha(), b.linha()));
            return new ImportacaoResponse(lidas, 0, 0, List.copyOf(erros.subList(0, Math.min(erros.size(), MAXIMO_ERROS_LISTADOS))));
        }
        Map<String, IndicadorHistorico> existentes = existentes();
        int inseridos = 0;
        for (Linha linha : validas) {
            if (!existentes.containsKey(linha.chave())) {
                inseridos++;
            }
            gravar(linha, fonte.trim(), existentes);
        }
        int atualizados = validas.size() - inseridos;
        auditoria.registrar(admin.id(), "indicador_historico", "importacao", "IMPORTAR", null,
                Map.of("fonte", fonte.trim(), "inseridos", inseridos, "atualizados", atualizados));
        return new ImportacaoResponse(lidas, inseridos, atualizados, List.of());
    }

    private IndicadorHistorico gravar(Linha l, String fonte, Map<String, IndicadorHistorico> existentes) {
        IndicadorHistorico h = existentes.get(l.chave());
        if (h == null) {
            h = new IndicadorHistorico(l.indicador(), l.modalidade(), l.ano(), l.mes(), l.feriadoCodigo(),
                    l.tipoHospedagemCodigo());
            h.definirValor(l.valor(), l.observacao(), fonte);
            h = historico.save(h);
            existentes.put(l.chave(), h);
        }
        else {
            h.definirValor(l.valor(), l.observacao(), fonte);
        }
        return h;
    }

    /** Mesmas regras das restrições da tabela, com mensagens para o usuário. */
    private static List<String> validar(Linha l, Set<String> feriadosValidos, Set<String> tiposValidos) {
        List<String> erros = new ArrayList<>();
        if (l.ano() < 2000 || l.ano() > 2100) {
            erros.add("ano fora do intervalo de 2000 a 2100");
        }
        if ((l.mes() == null) == (l.feriadoCodigo() == null)) {
            erros.add("informe o mês ou o feriado (um dos dois)");
        }
        if (l.mes() != null && (l.mes() < 1 || l.mes() > 12)) {
            erros.add("mês deve ser de 1 a 12");
        }
        if (l.feriadoCodigo() != null && !feriadosValidos.contains(l.feriadoCodigo())) {
            erros.add("feriado inválido: " + l.feriadoCodigo() + " (veja /api/referencias)");
        }
        if (l.tipoHospedagemCodigo() != null && !tiposValidos.contains(l.tipoHospedagemCodigo())) {
            erros.add("tipo de hospedagem inválido: " + l.tipoHospedagemCodigo() + " (veja /api/referencias)");
        }
        if (l.valor().signum() < 0) {
            erros.add("o valor não pode ser negativo");
        }
        if (l.indicador() == Indicador.TAXA_OCUPACAO && l.valor().compareTo(CEM) > 0) {
            erros.add("a taxa de ocupação vai de 0 a 100");
        }
        if (l.valor().precision() - l.valor().scale() > 12) {
            erros.add("valor grande demais");
        }
        if (l.observacao() != null && l.observacao().length() > 300) {
            erros.add("observação com mais de 300 caracteres");
        }
        return erros;
    }

    private Map<String, IndicadorHistorico> existentes() {
        Map<String, IndicadorHistorico> mapa = new HashMap<>();
        for (IndicadorHistorico h : historico.findAll()) {
            mapa.put(chave(h.getIndicador(), h.getModalidade(), h.getAno(), h.getMes(), h.getFeriadoCodigo(),
                    h.getTipoHospedagemCodigo()), h);
        }
        return mapa;
    }

    private Set<String> codigosDeFeriado() {
        Set<String> codigos = new HashSet<>();
        for (FeriadoEvento f : feriados.findAll()) {
            codigos.add(f.getCodigo());
        }
        return codigos;
    }

    private Set<String> codigosDeTipo() {
        Set<String> codigos = new HashSet<>();
        for (TipoHospedagem t : tipos.findAll()) {
            codigos.add(t.getCodigo());
        }
        return codigos;
    }

    private static String chave(Indicador indicador, Modalidade modalidade, int ano, Integer mes, String feriado,
            String tipo) {
        return indicador + "|" + modalidade + "|" + ano + "|" + Objects.toString(mes, "") + "|"
                + Objects.toString(feriado, "") + "|" + Objects.toString(tipo, "");
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String primeiraMaiuscula(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
