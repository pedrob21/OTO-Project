package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.calculo.BaseCalculo;
import OTO.calculo.BaseCalculoService;
import OTO.calculo.Numeros;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.DadosSecretaria;
import OTO.estabelecimento.Estabelecimento;
import OTO.pesquisa.RodadaDtos.AberturaRequest;
import OTO.pesquisa.RodadaDtos.AcompanhamentoCampanha;
import OTO.pesquisa.RodadaDtos.AcompanhamentoResponse;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.RodadaDtos.PrazoRequest;
import OTO.pesquisa.RodadaDtos.RodadaRequest;
import OTO.pesquisa.RodadaDtos.RodadaResponse;
import OTO.pesquisa.RodadaDtos.SituacaoEstabelecimento;
import OTO.referencia.FeriadoEventoRepository;

/**
 * Ciclo de vida de uma pesquisa: RASCUNHO (monta as campanhas) → ABERTA (recebe
 * respostas; o parceiro pode corrigir) → ENCERRADA (as campanhas podem ser publicadas).
 */
@Service
public class RodadaService {

    private final RodadaPesquisaRepository rodadas;
    private final CampanhaRepository campanhas;
    private final FeriadoEventoRepository feriados;
    private final BaseCalculoService bases;
    private final AuditoriaService auditoria;

    public RodadaService(RodadaPesquisaRepository rodadas, CampanhaRepository campanhas,
            FeriadoEventoRepository feriados, BaseCalculoService bases, AuditoriaService auditoria) {
        this.rodadas = rodadas;
        this.campanhas = campanhas;
        this.feriados = feriados;
        this.bases = bases;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<RodadaResponse> listar(StatusRodada status) {
        List<RodadaPesquisa> lista = status == null
                ? rodadas.findAllByOrderByCriadoEmDesc()
                : rodadas.findByStatusOrderByCriadoEmDesc(status);
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Campanha>> porRodada = campanhas.findByRodadaIdIn(lista.stream().map(RodadaPesquisa::getId).toList())
                .stream()
                .sorted(Comparator.comparing(Campanha::getPeriodoInicio))
                .collect(Collectors.groupingBy(c -> c.getRodada().getId()));
        return lista.stream().map(r -> RodadaResponse.de(r, porRodada.getOrDefault(r.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public RodadaResponse buscar(Long id) {
        return RodadaResponse.de(carregar(id), campanhas.findByRodadaIdOrderByPeriodoInicio(id));
    }

    @Transactional
    public RodadaResponse criar(RodadaRequest requisicao, UsuarioLogado admin) {
        String codigo = requisicao.codigo().trim();
        if (rodadas.existsByCodigo(codigo)) {
            throw new RegraNegocioException("Já existe uma pesquisa com o código " + codigo + ".");
        }
        validarDatas(requisicao.dataAbertura(), requisicao.prazoResposta());
        RodadaPesquisa rodada = rodadas.save(new RodadaPesquisa(codigo, requisicao.titulo().trim(),
                requisicao.dataAbertura(), requisicao.prazoResposta(), admin.id()));
        auditoria.registrar(admin.id(), "rodada_pesquisa", rodada.getId(), "CRIAR", null, requisicao);
        return RodadaResponse.de(rodada, List.of());
    }

    @Transactional
    public RodadaResponse alterar(Long id, RodadaRequest requisicao, UsuarioLogado admin) {
        RodadaPesquisa rodada = carregar(id);
        exigirRascunho(rodada, "alterada");
        String codigo = requisicao.codigo().trim();
        if (rodadas.existsByCodigoAndIdNot(codigo, id)) {
            throw new RegraNegocioException("Já existe outra pesquisa com o código " + codigo + ".");
        }
        validarDatas(requisicao.dataAbertura(), requisicao.prazoResposta());
        rodada.alterar(codigo, requisicao.titulo().trim(), requisicao.dataAbertura(), requisicao.prazoResposta());
        auditoria.registrar(admin.id(), "rodada_pesquisa", id, "ATUALIZAR", null, requisicao);
        return buscar(id);
    }

    @Transactional
    public void excluir(Long id, UsuarioLogado admin) {
        RodadaPesquisa rodada = carregar(id);
        exigirRascunho(rodada, "excluída");
        campanhas.deleteAll(campanhas.findByRodadaIdOrderByPeriodoInicio(id));
        rodadas.delete(rodada);
        auditoria.registrar(admin.id(), "rodada_pesquisa", id, "EXCLUIR", null, null);
    }

    @Transactional
    public CampanhaResponse adicionarCampanha(Long rodadaId, DadosCampanha dados, UsuarioLogado admin) {
        RodadaPesquisa rodada = carregar(rodadaId);
        exigirRascunho(rodada, "alterada");
        DadosCampanha validados = validarCampanha(dados, null);
        Campanha campanha = campanhas.save(new Campanha(rodada, validados));
        auditoria.registrar(admin.id(), "campanha", campanha.getId(), "CRIAR", null, validados);
        return CampanhaResponse.de(campanha);
    }

    @Transactional
    public CampanhaResponse alterarCampanha(Long rodadaId, Long campanhaId, DadosCampanha dados, UsuarioLogado admin) {
        exigirRascunho(carregar(rodadaId), "alterada");
        Campanha campanha = carregarCampanha(rodadaId, campanhaId);
        DadosCampanha validados = validarCampanha(dados, campanhaId);
        campanha.alterar(validados);
        auditoria.registrar(admin.id(), "campanha", campanhaId, "ATUALIZAR", null, validados);
        return CampanhaResponse.de(campanha);
    }

    @Transactional
    public void excluirCampanha(Long rodadaId, Long campanhaId, UsuarioLogado admin) {
        exigirRascunho(carregar(rodadaId), "alterada");
        campanhas.delete(carregarCampanha(rodadaId, campanhaId));
        auditoria.registrar(admin.id(), "campanha", campanhaId, "EXCLUIR", null, null);
    }

    @Transactional
    public RodadaResponse abrir(Long id, AberturaRequest requisicao, UsuarioLogado admin) {
        RodadaPesquisa rodada = carregar(id);
        if (!rodada.isRascunho()) {
            throw new RegraNegocioException("A pesquisa " + rodada.getCodigo() + " já foi aberta.");
        }
        List<Campanha> lista = campanhas.findByRodadaIdOrderByPeriodoInicio(id);
        if (lista.isEmpty()) {
            throw new RegraNegocioException("Inclua ao menos uma campanha antes de abrir a pesquisa.");
        }
        LocalDate abertura = requisicao.dataAbertura() == null ? LocalDate.now() : requisicao.dataAbertura();
        validarDatas(abertura, requisicao.prazoResposta());
        rodada.abrir(abertura, requisicao.prazoResposta());
        auditoria.registrar(admin.id(), "rodada_pesquisa", id, "ABRIR", null, requisicao);
        return RodadaResponse.de(rodada, lista);
    }

    @Transactional
    public RodadaResponse prorrogar(Long id, PrazoRequest requisicao, UsuarioLogado admin) {
        RodadaPesquisa rodada = carregar(id);
        if (!rodada.isAberta()) {
            throw new RegraNegocioException("Só é possível prorrogar uma pesquisa aberta.");
        }
        validarDatas(rodada.getDataAbertura(), requisicao.prazoResposta());
        LocalDate prazoAnterior = rodada.getPrazoResposta();
        rodada.prorrogar(requisicao.prazoResposta());
        auditoria.registrar(admin.id(), "rodada_pesquisa", id, "PRORROGAR", Map.of("prazoResposta", prazoAnterior),
                requisicao);
        return buscar(id);
    }

    @Transactional
    public RodadaResponse encerrar(Long id, UsuarioLogado admin) {
        RodadaPesquisa rodada = carregar(id);
        if (!rodada.isAberta()) {
            throw new RegraNegocioException("Só é possível encerrar uma pesquisa aberta.");
        }
        rodada.encerrar();
        auditoria.registrar(admin.id(), "rodada_pesquisa", id, "ENCERRAR", null, null);
        return buscar(id);
    }

    /** Volta a receber respostas. Não vale para pesquisas com campanha já publicada. */
    @Transactional
    public RodadaResponse reabrir(Long id, PrazoRequest requisicao, UsuarioLogado admin) {
        RodadaPesquisa rodada = carregar(id);
        if (!rodada.isEncerrada()) {
            throw new RegraNegocioException("Só é possível reabrir uma pesquisa encerrada.");
        }
        if (campanhas.existsByRodadaIdAndPublicadaEmIsNotNull(id)) {
            throw new RegraNegocioException("A pesquisa tem campanha publicada. Cancele a publicação antes de reabrir.");
        }
        validarDatas(rodada.getDataAbertura(), requisicao.prazoResposta());
        rodada.reabrir(requisicao.prazoResposta());
        auditoria.registrar(admin.id(), "rodada_pesquisa", id, "REABRIR", null, requisicao);
        return buscar(id);
    }

    @Transactional(readOnly = true)
    public AcompanhamentoResponse acompanhamento(Long id) {
        RodadaPesquisa rodada = carregar(id);
        List<AcompanhamentoCampanha> lista = campanhas.findByRodadaIdOrderByPeriodoInicio(id).stream()
                .map(c -> acompanhar(bases.montar(c)))
                .toList();
        return new AcompanhamentoResponse(rodada.getId(), rodada.getCodigo(), rodada.getTitulo(), rodada.getStatus(),
                rodada.getPrazoResposta(), lista);
    }

    private static AcompanhamentoCampanha acompanhar(BaseCalculo base) {
        List<SituacaoEstabelecimento> situacoes = new ArrayList<>();
        for (BaseCalculo.Participante p : base.participantes()) {
            situacoes.add(situacao(p.estabelecimento(), p.capacidade().getQtdLeitos(), p.resposta(), false));
        }
        Map<Long, RespostaOcupacao> desconsideradas = base.respostasDesconsideradas().stream()
                .collect(Collectors.toMap(r -> r.getEstabelecimento().getId(), Function.identity()));
        for (Estabelecimento e : base.semCapacidade()) {
            situacoes.add(situacao(e, null, desconsideradas.get(e.getId()), true));
        }
        situacoes.sort(Comparator.comparing(SituacaoEstabelecimento::nomeFantasia));

        BigDecimal percentual = Numeros.duasCasas(base.percentualLeitosRespondentes());
        BigDecimal minimo = base.parametro().getPercentualMinimoLeitosPublicacao();
        Campanha campanha = base.campanha();
        return new AcompanhamentoCampanha(campanha.getId(), campanha.getNome(), base.participantes().size(),
                base.qtdRespondentes(), base.leitosTotal(), base.leitosRespondentes(), percentual, minimo,
                percentual.compareTo(minimo) >= 0, List.copyOf(situacoes));
    }

    private static SituacaoEstabelecimento situacao(Estabelecimento e, Integer qtdLeitos, RespostaOcupacao resposta,
            boolean semCapacidade) {
        DadosSecretaria s = e.getDadosSecretaria();
        return new SituacaoEstabelecimento(e.getId(), e.getNomeFantasia(), e.getTipoHospedagemCodigo(), qtdLeitos,
                s.aceitaPesquisas(), s.contatoNome(), s.telefonePesquisas(), s.emailPesquisas(), resposta != null,
                resposta == null ? null : resposta.getVersao(), resposta == null ? null : resposta.getEnviadoEm(),
                semCapacidade);
    }

    /** Confere as regras da campanha e devolve os dados normalizados (feriado só em FERIADO). */
    private DadosCampanha validarCampanha(DadosCampanha d, Long campanhaIdAtual) {
        if (d.periodoFim().isBefore(d.periodoInicio())) {
            throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
        }
        long dias = ChronoUnit.DAYS.between(d.periodoInicio(), d.periodoFim()) + 1;
        if (d.qtdDiarias() > dias) {
            throw new RegraNegocioException("A quantidade de diárias (" + d.qtdDiarias()
                    + ") passa o número de dias do período (" + dias + ").");
        }
        String feriado = null;
        if (d.tipoPeriodo() == TipoPeriodo.FERIADO) {
            if (d.feriadoCodigo() == null || d.feriadoCodigo().isBlank()) {
                throw new RegraNegocioException("Informe o feriado (feriadoCodigo) da campanha.");
            }
            feriado = d.feriadoCodigo().trim();
            if (!feriados.existsById(feriado)) {
                throw new RegraNegocioException("Feriado inválido: " + feriado + ". Veja /api/referencias.");
            }
        }
        else {
            YearMonth mes = YearMonth.from(d.periodoInicio());
            if (d.periodoInicio().getDayOfMonth() != 1 || !d.periodoFim().equals(mes.atEndOfMonth())) {
                throw new RegraNegocioException(
                        "A campanha mensal vai do primeiro ao último dia do mês (ex.: 2026-02-01 a 2026-02-28).");
            }
        }
        String feriadoFinal = feriado;
        boolean repetida = campanhas.findByModalidadeAndTipoPeriodoAndPeriodoInicio(d.modalidade(), d.tipoPeriodo(),
                        d.periodoInicio()).stream()
                .anyMatch(c -> !c.getId().equals(campanhaIdAtual) && Objects.equals(c.getFeriadoCodigo(), feriadoFinal));
        if (repetida) {
            throw new RegraNegocioException("Já existe uma campanha " + d.modalidade() + " para este período.");
        }
        return new DadosCampanha(d.nome().trim(), d.modalidade(), d.tipoPeriodo(), feriado, d.periodoInicio(),
                d.periodoFim(), d.qtdDiarias());
    }

    private static void validarDatas(LocalDate abertura, LocalDate prazo) {
        if (abertura != null && prazo != null && prazo.isBefore(abertura)) {
            throw new RegraNegocioException("O prazo de resposta não pode ser anterior à data de abertura.");
        }
    }

    private static void exigirRascunho(RodadaPesquisa rodada, String acao) {
        if (!rodada.isRascunho()) {
            throw new RegraNegocioException("A pesquisa " + rodada.getCodigo() + " já foi aberta e não pode mais ser "
                    + acao + ".");
        }
    }

    private RodadaPesquisa carregar(Long id) {
        return rodadas.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Pesquisa", id));
    }

    private Campanha carregarCampanha(Long rodadaId, Long campanhaId) {
        Campanha campanha = campanhas.findById(campanhaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Campanha", campanhaId));
        if (!campanha.getRodada().getId().equals(rodadaId)) {
            throw RecursoNaoEncontradoException.de("Campanha", campanhaId);
        }
        return campanha;
    }
}
