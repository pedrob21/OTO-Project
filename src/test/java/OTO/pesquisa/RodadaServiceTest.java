package OTO.pesquisa;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.calculo.BaseCalculo;
import OTO.calculo.BaseCalculoService;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
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

@ExtendWith(MockitoExtension.class)
class RodadaServiceTest {

    private static final YearMonth FEV = YearMonth.of(2026, 2);
    private static final LocalDate HOJE = LocalDate.now();

    @Mock
    private RodadaPesquisaRepository rodadas;
    @Mock
    private CampanhaRepository campanhas;
    @Mock
    private FeriadoEventoRepository feriados;
    @Mock
    private BaseCalculoService bases;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private RodadaService servico;

    private RodadaPesquisa rascunho() {
        RodadaPesquisa r = Fixtures.rodada(1, "005/26", StatusRodada.RASCUNHO);
        when(rodadas.findById(1L)).thenReturn(Optional.of(r));
        return r;
    }

    private RodadaPesquisa naSituacao(StatusRodada status) {
        RodadaPesquisa r = Fixtures.rodada(1, "005/26", status);
        when(rodadas.findById(1L)).thenReturn(Optional.of(r));
        return r;
    }

    // ------------------------------------------------------------------ consulta

    @Test
    void listarJuntaAsCampanhasDeCadaRodadaPeloInicio() {
        RodadaPesquisa r1 = Fixtures.rodada(1, "001/26", StatusRodada.ABERTA);
        RodadaPesquisa r2 = Fixtures.rodada(2, "002/26", StatusRodada.RASCUNHO);
        when(rodadas.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(r1, r2));
        when(campanhas.findByRodadaIdIn(List.of(1L, 2L))).thenReturn(List.of(
                Fixtures.campanhaMensal(11, r1, YearMonth.of(2026, 3)), Fixtures.campanhaMensal(10, r1, YearMonth.of(2026, 1))));

        List<RodadaResponse> lista = servico.listar(null);

        assertThat(lista).extracting(RodadaResponse::codigo).containsExactly("001/26", "002/26");
        assertThat(lista.get(0).campanhas()).extracting(CampanhaResponse::id).containsExactly(10L, 11L);
        assertThat(lista.get(1).campanhas()).isEmpty();
    }

    @Test
    void listarPorSituacaoSemResultado() {
        when(rodadas.findByStatusOrderByCriadoEmDesc(StatusRodada.ENCERRADA)).thenReturn(List.of());
        assertThat(servico.listar(StatusRodada.ENCERRADA)).isEmpty();
        verify(campanhas, never()).findByRodadaIdIn(any());
    }

    @Test
    void buscarInexistente() {
        when(rodadas.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.buscar(9L)).isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pesquisa 9 não encontrado(a).");
    }

    // ------------------------------------------------------------------ cadastro

    @Test
    void criarEmRascunho() {
        when(rodadas.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 7L));
        RodadaRequest requisicao = new RodadaRequest(" 005/26 ", " Fevereiro ", null, null);

        RodadaResponse criada = servico.criar(requisicao, ADMIN);

        assertThat(criada.id()).isEqualTo(7L);
        assertThat(criada.codigo()).isEqualTo("005/26");
        assertThat(criada.titulo()).isEqualTo("Fevereiro");
        assertThat(criada.status()).isEqualTo(StatusRodada.RASCUNHO);
        verify(auditoria).registrar(1L, "rodada_pesquisa", 7L, "CRIAR", null, requisicao);
    }

    @Test
    void criarComCodigoRepetidoOuPrazoAntesDaAbertura() {
        when(rodadas.existsByCodigo("005/26")).thenReturn(true);
        assertThatThrownBy(() -> servico.criar(new RodadaRequest("005/26", "T", null, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("código 005/26");

        assertThatThrownBy(() -> servico.criar(new RodadaRequest("006/26", "T", HOJE, HOJE.minusDays(1)), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("prazo de resposta");
        verify(rodadas, never()).save(any());
    }

    @Test
    void alterarRascunho() {
        RodadaPesquisa r = rascunho();

        RodadaResponse alterada = servico.alterar(1L, new RodadaRequest("005/26", "Novo título", HOJE, HOJE.plusDays(5)), ADMIN);

        assertThat(alterada.titulo()).isEqualTo("Novo título");
        assertThat(r.getPrazoResposta()).isEqualTo(HOJE.plusDays(5));
    }

    @Test
    void alterarRodadaAbertaOuComCodigoDeOutra() {
        naSituacao(StatusRodada.ABERTA);
        assertThatThrownBy(() -> servico.alterar(1L, new RodadaRequest("005/26", "T", null, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A pesquisa 005/26 já foi aberta e não pode mais ser alterada.");
    }

    @Test
    void alterarParaCodigoDeOutraRodada() {
        rascunho();
        when(rodadas.existsByCodigoAndIdNot("004/26", 1L)).thenReturn(true);
        assertThatThrownBy(() -> servico.alterar(1L, new RodadaRequest("004/26", "T", null, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void excluirRascunhoApagaAsCampanhas() {
        RodadaPesquisa r = rascunho();
        List<Campanha> lista = List.of(Fixtures.campanhaMensal(10, r, FEV));
        when(campanhas.findByRodadaIdOrderByPeriodoInicio(1L)).thenReturn(lista);

        servico.excluir(1L, ADMIN);

        verify(campanhas).deleteAll(lista);
        verify(rodadas).delete(r);
        verify(auditoria).registrar(1L, "rodada_pesquisa", 1L, "EXCLUIR", null, null);
    }

    @Test
    void naoExcluiRodadaAberta() {
        naSituacao(StatusRodada.ABERTA);
        assertThatThrownBy(() -> servico.excluir(1L, ADMIN)).isInstanceOf(RegraNegocioException.class);
        verify(rodadas, never()).delete(any());
    }

    // ------------------------------------------------------------------ campanhas

    @Test
    void adicionarCampanhaMensalIgnoraOFeriado() {
        rascunho();
        when(campanhas.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 10L));
        DadosCampanha dados = new DadosCampanha(" Fevereiro ", Modalidade.CONSOLIDADA, TipoPeriodo.MENSAL, "CARNAVAL",
                FEV.atDay(1), FEV.atEndOfMonth(), 28);

        CampanhaResponse criada = servico.adicionarCampanha(1L, dados, ADMIN);

        assertThat(criada.id()).isEqualTo(10L);
        assertThat(criada.nome()).isEqualTo("Fevereiro");
        assertThat(criada.feriadoCodigo()).isNull();
        assertThat(criada.qtdDiarias()).isEqualTo(28);
        verify(auditoria).registrar(eq(1L), eq("campanha"), eq(10L), eq("CRIAR"), isNull(), any(DadosCampanha.class));
    }

    @Test
    void adicionarCampanhaDeFeriado() {
        rascunho();
        when(feriados.existsById("CARNAVAL")).thenReturn(true);
        when(campanhas.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 10L));

        CampanhaResponse criada = servico.adicionarCampanha(1L, new DadosCampanha("Carnaval", Modalidade.EXPECTATIVA,
                TipoPeriodo.FERIADO, " CARNAVAL ", LocalDate.of(2026, 2, 13), LocalDate.of(2026, 2, 18), 5), ADMIN);

        assertThat(criada.feriadoCodigo()).isEqualTo("CARNAVAL");
        assertThat(criada.tipoPeriodo()).isEqualTo(TipoPeriodo.FERIADO);
    }

    @Test
    void regrasDaCampanha() {
        rascunho();
        LocalDate ini = LocalDate.of(2026, 2, 13);

        assertThatThrownBy(() -> servico.adicionarCampanha(1L, new DadosCampanha("X", Modalidade.CONSOLIDADA,
                TipoPeriodo.FERIADO, "CARNAVAL", ini, ini.minusDays(1), 1), ADMIN))
                .hasMessageContaining("fim do período");
        assertThatThrownBy(() -> servico.adicionarCampanha(1L, new DadosCampanha("X", Modalidade.CONSOLIDADA,
                TipoPeriodo.FERIADO, "CARNAVAL", ini, ini.plusDays(2), 4), ADMIN))
                .hasMessageContaining("quantidade de diárias (4) passa o número de dias do período (3)");
        assertThatThrownBy(() -> servico.adicionarCampanha(1L, new DadosCampanha("X", Modalidade.CONSOLIDADA,
                TipoPeriodo.FERIADO, " ", ini, ini.plusDays(2), 2), ADMIN))
                .hasMessageContaining("Informe o feriado");
        assertThatThrownBy(() -> servico.adicionarCampanha(1L, new DadosCampanha("X", Modalidade.CONSOLIDADA,
                TipoPeriodo.FERIADO, "NATAL", ini, ini.plusDays(2), 2), ADMIN))
                .hasMessageContaining("Feriado inválido: NATAL");
        assertThatThrownBy(() -> servico.adicionarCampanha(1L, new DadosCampanha("X", Modalidade.CONSOLIDADA,
                TipoPeriodo.MENSAL, null, FEV.atDay(2), FEV.atEndOfMonth(), 2), ADMIN))
                .hasMessageContaining("primeiro ao último dia do mês");
        verify(campanhas, never()).save(any());
    }

    @Test
    void campanhaRepetidaNaoPode() {
        RodadaPesquisa r = rascunho();
        when(campanhas.findByModalidadeAndTipoPeriodoAndPeriodoInicio(Modalidade.CONSOLIDADA, TipoPeriodo.MENSAL,
                FEV.atDay(1))).thenReturn(List.of(Fixtures.campanhaMensal(99, r, FEV)));

        assertThatThrownBy(() -> servico.adicionarCampanha(1L,
                Fixtures.dadosMensal("Fev", Modalidade.CONSOLIDADA, FEV, 28), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe uma campanha CONSOLIDADA para este período.");
    }

    @Test
    void alterarCampanhaNaoConflitaComElaMesma() {
        RodadaPesquisa r = rascunho();
        Campanha campanha = Fixtures.campanhaMensal(10, r, FEV);
        when(campanhas.findById(10L)).thenReturn(Optional.of(campanha));
        when(campanhas.findByModalidadeAndTipoPeriodoAndPeriodoInicio(Modalidade.CONSOLIDADA, TipoPeriodo.MENSAL,
                FEV.atDay(1))).thenReturn(List.of(campanha));

        CampanhaResponse alterada = servico.alterarCampanha(1L, 10L,
                Fixtures.dadosMensal("Fevereiro revisado", Modalidade.CONSOLIDADA, FEV, 20), ADMIN);

        assertThat(alterada.nome()).isEqualTo("Fevereiro revisado");
        assertThat(alterada.qtdDiarias()).isEqualTo(20);
    }

    @Test
    void campanhaDeOutraRodadaNaoEhEncontrada() {
        rascunho();
        Campanha deOutra = Fixtures.campanhaMensal(10, Fixtures.rodada(2, "X", StatusRodada.RASCUNHO), FEV);
        when(campanhas.findById(10L)).thenReturn(Optional.of(deOutra));

        assertThatThrownBy(() -> servico.excluirCampanha(1L, 10L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(campanhas, never()).delete(any());
    }

    @Test
    void excluirCampanha() {
        RodadaPesquisa r = rascunho();
        Campanha campanha = Fixtures.campanhaMensal(10, r, FEV);
        when(campanhas.findById(10L)).thenReturn(Optional.of(campanha));

        servico.excluirCampanha(1L, 10L, ADMIN);

        verify(campanhas).delete(campanha);
        verify(auditoria).registrar(1L, "campanha", 10L, "EXCLUIR", null, null);
    }

    @Test
    void naoMexeEmCampanhaDeRodadaAberta() {
        naSituacao(StatusRodada.ABERTA);
        assertThatThrownBy(() -> servico.adicionarCampanha(1L,
                Fixtures.dadosMensal("Fev", Modalidade.CONSOLIDADA, FEV, 28), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
        verify(campanhas, never()).findById(anyLong());
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Test
    void abrirSemDataUsaHoje() {
        RodadaPesquisa r = rascunho();
        when(campanhas.findByRodadaIdOrderByPeriodoInicio(1L)).thenReturn(List.of(Fixtures.campanhaMensal(10, r, FEV)));
        AberturaRequest requisicao = new AberturaRequest(null, HOJE.plusDays(10));

        RodadaResponse aberta = servico.abrir(1L, requisicao, ADMIN);

        assertThat(aberta.status()).isEqualTo(StatusRodada.ABERTA);
        assertThat(aberta.dataAbertura()).isEqualTo(HOJE);
        assertThat(aberta.campanhas()).hasSize(1);
        verify(auditoria).registrar(1L, "rodada_pesquisa", 1L, "ABRIR", null, requisicao);
    }

    @Test
    void abrirExigeRascunhoECampanhasEPrazoValido() {
        RodadaPesquisa r = rascunho();
        when(campanhas.findByRodadaIdOrderByPeriodoInicio(1L)).thenReturn(List.of())
                .thenReturn(List.of(Fixtures.campanhaMensal(10, r, FEV)));

        assertThatThrownBy(() -> servico.abrir(1L, new AberturaRequest(null, HOJE.plusDays(1)), ADMIN))
                .hasMessageContaining("ao menos uma campanha");
        assertThatThrownBy(() -> servico.abrir(1L, new AberturaRequest(HOJE, HOJE.minusDays(1)), ADMIN))
                .hasMessageContaining("prazo de resposta");
        assertThat(r.isRascunho()).isTrue();

        r.abrir(HOJE, HOJE.plusDays(1));
        assertThatThrownBy(() -> servico.abrir(1L, new AberturaRequest(null, HOJE.plusDays(1)), ADMIN))
                .hasMessageContaining("já foi aberta");
    }

    @Test
    void prorrogarRodadaAberta() {
        RodadaPesquisa r = naSituacao(StatusRodada.ABERTA);
        LocalDate prazoAnterior = r.getPrazoResposta();
        PrazoRequest requisicao = new PrazoRequest(prazoAnterior.plusDays(5));

        RodadaResponse prorrogada = servico.prorrogar(1L, requisicao, ADMIN);

        assertThat(prorrogada.prazoResposta()).isEqualTo(prazoAnterior.plusDays(5));
        verify(auditoria).registrar(1L, "rodada_pesquisa", 1L, "PRORROGAR", Map.of("prazoResposta", prazoAnterior),
                requisicao);
    }

    @Test
    void soProrrogaOuEncerraRodadaAberta() {
        naSituacao(StatusRodada.ENCERRADA);
        assertThatThrownBy(() -> servico.prorrogar(1L, new PrazoRequest(HOJE), ADMIN))
                .hasMessage("Só é possível prorrogar uma pesquisa aberta.");
        assertThatThrownBy(() -> servico.encerrar(1L, ADMIN))
                .hasMessage("Só é possível encerrar uma pesquisa aberta.");
    }

    @Test
    void encerrar() {
        RodadaPesquisa r = naSituacao(StatusRodada.ABERTA);
        RodadaResponse encerrada = servico.encerrar(1L, ADMIN);
        assertThat(encerrada.status()).isEqualTo(StatusRodada.ENCERRADA);
        assertThat(r.getEncerradaEm()).isNotNull();
        verify(auditoria).registrar(1L, "rodada_pesquisa", 1L, "ENCERRAR", null, null);
    }

    @Test
    void reabrirRodadaEncerradaSemPublicacao() {
        RodadaPesquisa r = naSituacao(StatusRodada.ENCERRADA);
        LocalDate novoPrazo = r.getDataAbertura().plusDays(30);

        RodadaResponse reaberta = servico.reabrir(1L, new PrazoRequest(novoPrazo), ADMIN);

        assertThat(reaberta.status()).isEqualTo(StatusRodada.ABERTA);
        assertThat(reaberta.prazoResposta()).isEqualTo(novoPrazo);
        assertThat(r.getEncerradaEm()).isNull();
    }

    @Test
    void reabrirExigeEncerradaESemCampanhaPublicada() {
        naSituacao(StatusRodada.ABERTA);
        assertThatThrownBy(() -> servico.reabrir(1L, new PrazoRequest(HOJE), ADMIN))
                .hasMessage("Só é possível reabrir uma pesquisa encerrada.");

        naSituacao(StatusRodada.ENCERRADA);
        when(campanhas.existsByRodadaIdAndPublicadaEmIsNotNull(1L)).thenReturn(true);
        assertThatThrownBy(() -> servico.reabrir(1L, new PrazoRequest(HOJE), ADMIN))
                .hasMessageContaining("campanha publicada");
    }

    // ------------------------------------------------------------------ acompanhamento

    @Test
    void acompanhamentoMostraQuemRespondeuEQuemFalta() {
        RodadaPesquisa r = naSituacao(StatusRodada.ABERTA);
        Campanha campanha = Fixtures.campanhaMensal(10, r, FEV);
        Estabelecimento a = Fixtures.estabelecimento(1, "Hotel B", "HOTEL");
        Estabelecimento b = Fixtures.estabelecimento(2, "Hotel A", "HOTEL");
        Estabelecimento c = Fixtures.estabelecimento(3, "Pousada C", "POUSADA");
        RespostaOcupacao respostaA = Fixtures.resposta(100, campanha, a, "70", null);
        RespostaOcupacao respostaC = Fixtures.resposta(101, campanha, c, "50", null);
        BaseCalculo base = new BaseCalculo(campanha, FEV.atEndOfMonth(),
                List.of(new BaseCalculo.Participante(a, Fixtures.capacidade(1, a, FEV.atDay(1), 10, 30, 4), respostaA),
                        new BaseCalculo.Participante(b, Fixtures.capacidade(2, b, FEV.atDay(1), 10, 20, 4), null)),
                List.of(c), List.of(respostaC), null, Fixtures.parametro(1, FEV.atDay(1)), List.of(), List.of());
        when(campanhas.findByRodadaIdOrderByPeriodoInicio(1L)).thenReturn(List.of(campanha));
        when(bases.montar(campanha)).thenReturn(base);

        AcompanhamentoResponse acompanhamento = servico.acompanhamento(1L);

        assertThat(acompanhamento.codigo()).isEqualTo("005/26");
        AcompanhamentoCampanha linha = acompanhamento.campanhas().get(0);
        assertThat(linha.qtdMh()).isEqualTo(2);
        assertThat(linha.qtdRespondentes()).isEqualTo(1);
        assertThat(linha.leitosTotal()).isEqualTo(50);
        assertThat(linha.leitosRespondentes()).isEqualTo(30);
        assertThat(linha.percentualLeitosRespondentes()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(linha.atingiuMinimo()).isTrue();
        assertThat(linha.estabelecimentos()).extracting(SituacaoEstabelecimento::nomeFantasia)
                .containsExactly("Hotel A", "Hotel B", "Pousada C");
        SituacaoEstabelecimento hotelA = linha.estabelecimentos().get(0);
        assertThat(hotelA.respondeu()).isFalse();
        assertThat(hotelA.qtdLeitos()).isEqualTo(20);
        assertThat(hotelA.contatoNome()).isEqualTo("Maria");
        SituacaoEstabelecimento hotelB = linha.estabelecimentos().get(1);
        assertThat(hotelB.respondeu()).isTrue();
        assertThat(hotelB.versao()).isEqualTo(1);
        SituacaoEstabelecimento pousada = linha.estabelecimentos().get(2);
        assertThat(pousada.semCapacidade()).isTrue();
        assertThat(pousada.respondeu()).isTrue();
        assertThat(pousada.qtdLeitos()).isNull();
    }

    @Test
    void acompanhamentoAbaixoDoMinimo() {
        RodadaPesquisa r = naSituacao(StatusRodada.ABERTA);
        Campanha campanha = Fixtures.campanhaMensal(10, r, FEV);
        Estabelecimento a = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        BaseCalculo base = new BaseCalculo(campanha, FEV.atEndOfMonth(),
                List.of(new BaseCalculo.Participante(a, Fixtures.capacidade(1, a, FEV.atDay(1), 10, 30, 4), null)),
                List.of(), List.of(), null, Fixtures.parametro(1, FEV.atDay(1)), List.of(), List.of());
        when(campanhas.findByRodadaIdOrderByPeriodoInicio(1L)).thenReturn(List.of(campanha));
        when(bases.montar(campanha)).thenReturn(base);

        AcompanhamentoCampanha linha = servico.acompanhamento(1L).campanhas().get(0);

        assertThat(linha.percentualLeitosRespondentes()).isEqualByComparingTo("0");
        assertThat(linha.atingiuMinimo()).isFalse();
    }

    @Test
    void campanhaSalvaRecebeOsDadosValidados() {
        rascunho();
        when(campanhas.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 10L));

        servico.adicionarCampanha(1L, Fixtures.dadosMensal(" Fev ", Modalidade.CONSOLIDADA, FEV, 28), ADMIN);

        ArgumentCaptor<Campanha> salva = ArgumentCaptor.forClass(Campanha.class);
        verify(campanhas).save(salva.capture());
        assertThat(salva.getValue().getNome()).isEqualTo("Fev");
        assertThat(salva.getValue().getRodada().getId()).isEqualTo(1L);
    }
}
