package OTO.pesquisa;

import static OTO.Fixtures.ADMIN;
import static OTO.Fixtures.PARCEIRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import OTO.Fixtures;
import OTO.calculo.ResultadoCampanha;
import OTO.calculo.ResultadoCampanhaRepository;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.ControleAcesso;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.estabelecimento.Situacao;
import OTO.pesquisa.RespostaDtos.HistoricoRespostaResponse;
import OTO.pesquisa.RespostaDtos.MinhaRespostaResponse;
import OTO.pesquisa.RespostaDtos.PendenciaResponse;
import OTO.pesquisa.RespostaDtos.RespostaRequest;
import OTO.pesquisa.RespostaDtos.RespostaResponse;

@ExtendWith(MockitoExtension.class)
class RespostaServiceTest {

    private static final YearMonth FEV = YearMonth.of(2026, 2);
    private static final RespostaRequest REQUISICAO = new RespostaRequest(new BigDecimal("72.50"), new BigDecimal("450.00"));

    @Mock
    private RespostaOcupacaoRepository respostas;
    @Mock
    private RespostaOcupacaoHistoricoRepository historicos;
    @Mock
    private CampanhaRepository campanhas;
    @Mock
    private EstabelecimentoRepository estabelecimentos;
    @Mock
    private ResultadoCampanhaRepository resultados;
    @Mock
    private ControleAcesso acesso;

    @InjectMocks
    private RespostaService servico;

    private final Estabelecimento hotel = Fixtures.estabelecimento(5, "Hotel A", "HOTEL");

    private Campanha campanha(StatusRodada status) {
        Campanha c = Fixtures.campanhaMensal(10, Fixtures.rodada(1, "005/26", status), FEV);
        when(campanhas.findById(10L)).thenReturn(Optional.of(c));
        return c;
    }

    // ------------------------------------------------------------------ envio

    @Test
    void primeiroEnvioCriaAVersaoUmEGuardaNoHistorico() {
        Campanha c = campanha(StatusRodada.ABERTA);
        when(estabelecimentos.findById(5L)).thenReturn(Optional.of(hotel));
        when(respostas.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 100L));

        RespostaResponse resposta = servico.responder(10L, 5L, REQUISICAO, PARCEIRO);

        verify(acesso).exigirEscrita(PARCEIRO, 5L);
        assertThat(resposta.id()).isEqualTo(100L);
        assertThat(resposta.campanhaId()).isEqualTo(c.getId());
        assertThat(resposta.versao()).isEqualTo(1);
        assertThat(resposta.taxaOcupacao()).isEqualByComparingTo("72.50");
        assertThat(resposta.enviadoPor()).isEqualTo(2L);
        ArgumentCaptor<RespostaOcupacaoHistorico> historico = ArgumentCaptor.forClass(RespostaOcupacaoHistorico.class);
        verify(historicos).save(historico.capture());
        assertThat(historico.getValue().getRespostaId()).isEqualTo(100L);
        assertThat(historico.getValue().getVersao()).isEqualTo(1);
    }

    @Test
    void novoEnvioCorrigeESomaUmaVersao() {
        Campanha c = campanha(StatusRodada.ABERTA);
        RespostaOcupacao atual = Fixtures.resposta(100, c, hotel, "60", null);
        when(estabelecimentos.findById(5L)).thenReturn(Optional.of(hotel));
        when(respostas.findByCampanhaIdAndEstabelecimentoId(10L, 5L)).thenReturn(Optional.of(atual));

        RespostaResponse resposta = servico.responder(10L, 5L, REQUISICAO, PARCEIRO);

        assertThat(resposta.versao()).isEqualTo(2);
        assertThat(atual.getTaxaOcupacao()).isEqualByComparingTo("72.50");
        verify(respostas, never()).save(any());
        verify(historicos).save(any());
    }

    @Test
    void adminLancaRespostaDepoisDoEncerramento() {
        campanha(StatusRodada.ENCERRADA);
        when(estabelecimentos.findById(5L)).thenReturn(Optional.of(hotel));
        when(respostas.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 100L));

        assertThat(servico.responder(10L, 5L, REQUISICAO, ADMIN).enviadoPor()).isEqualTo(1L);
    }

    @Test
    void parceiroNaoRespondeDepoisDoEncerramentoENinguemRespondeRascunho() {
        campanha(StatusRodada.ENCERRADA);
        assertThatThrownBy(() -> servico.responder(10L, 5L, REQUISICAO, PARCEIRO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A pesquisa 005/26 não está aberta para respostas.");

        campanha(StatusRodada.RASCUNHO);
        assertThatThrownBy(() -> servico.responder(10L, 5L, REQUISICAO, ADMIN)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void campanhaPublicadaNaoRecebeResposta() {
        campanha(StatusRodada.ENCERRADA).publicar(1L);
        assertThatThrownBy(() -> servico.responder(10L, 5L, REQUISICAO, ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi publicada");
    }

    @Test
    void estabelecimentoForaDoCalculoNaoResponde() {
        campanha(StatusRodada.ABERTA);
        when(estabelecimentos.findById(5L)).thenReturn(Optional.of(Fixtures.estabelecimento(5, "X", "HOTEL", Situacao.IRREGULAR)));
        assertThatThrownBy(() -> servico.responder(10L, 5L, REQUISICAO, ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo ou irregular");
    }

    @Test
    void estabelecimentoInexistente() {
        campanha(StatusRodada.ABERTA);
        when(estabelecimentos.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.responder(10L, 5L, REQUISICAO, ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void parceiroSemVinculoNaoResponde() {
        doThrow(new AccessDeniedException("não")).when(acesso).exigirEscrita(PARCEIRO, 5L);
        assertThatThrownBy(() -> servico.responder(10L, 5L, REQUISICAO, PARCEIRO)).isInstanceOf(AccessDeniedException.class);
        verify(campanhas, never()).findById(anyLong());
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void respostasDaCampanhaOrdenadasPeloNome() {
        Campanha c = campanha(StatusRodada.ABERTA);
        when(respostas.findByCampanhaIdComEstabelecimento(10L)).thenReturn(List.of(
                Fixtures.resposta(1, c, Fixtures.estabelecimento(1, "Pousada", "POUSADA"), "50", null),
                Fixtures.resposta(2, c, Fixtures.estabelecimento(2, "Hotel", "HOTEL"), "60", null)));

        assertThat(servico.listarDaCampanha(10L)).extracting(RespostaResponse::nomeFantasia)
                .containsExactly("Hotel", "Pousada");
    }

    @Test
    void historicoDaRespostaConfereOAcesso() {
        Campanha c = Fixtures.campanhaMensal(10, Fixtures.rodada(1, "005/26", StatusRodada.ABERTA), FEV);
        RespostaOcupacao resposta = Fixtures.resposta(100, c, hotel, "60", null);
        when(respostas.findById(100L)).thenReturn(Optional.of(resposta));
        RespostaOcupacaoHistorico v1 = RespostaOcupacaoHistorico.de(resposta);
        when(historicos.findByRespostaIdOrderByVersaoDesc(100L)).thenReturn(List.of(v1));

        List<HistoricoRespostaResponse> historico = servico.historico(100L, PARCEIRO);

        verify(acesso).exigirLeitura(PARCEIRO, 5L);
        assertThat(historico).extracting(HistoricoRespostaResponse::versao).containsExactly(1);
    }

    @Test
    void historicoDeRespostaInexistente() {
        when(respostas.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.historico(9L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void pendenciasCruzamCampanhasAbertasEEstabelecimentos() {
        RodadaPesquisa rodada = Fixtures.rodada(1, "005/26", StatusRodada.ABERTA);
        Campanha fev = Fixtures.campanhaMensal(10, rodada, FEV);
        Campanha mar = Fixtures.campanhaMensal(11, rodada, FEV.plusMonths(1));
        Estabelecimento irregular = Fixtures.estabelecimento(6, "Irregular", "HOTEL", Situacao.IRREGULAR);
        when(acesso.estabelecimentosDo(PARCEIRO)).thenReturn(List.of(5L, 6L));
        when(estabelecimentos.findByIdInOrderByNomeFantasia(List.of(5L, 6L))).thenReturn(List.of(hotel, irregular));
        when(campanhas.findByStatusDaRodada(StatusRodada.ABERTA)).thenReturn(List.of(fev, mar));
        when(respostas.findByCampanhaIdsComEstabelecimento(List.of(10L, 11L)))
                .thenReturn(List.of(Fixtures.resposta(100, fev, hotel, "65", "400")));

        List<PendenciaResponse> pendencias = servico.pendencias(PARCEIRO);

        assertThat(pendencias).hasSize(2);
        assertThat(pendencias.get(0).campanhaId()).isEqualTo(10L);
        assertThat(pendencias.get(0).respondida()).isTrue();
        assertThat(pendencias.get(0).taxaOcupacao()).isEqualByComparingTo("65");
        assertThat(pendencias.get(0).rodadaCodigo()).isEqualTo("005/26");
        assertThat(pendencias.get(1).campanhaId()).isEqualTo(11L);
        assertThat(pendencias.get(1).respondida()).isFalse();
        assertThat(pendencias.get(1).respostaId()).isNull();
    }

    @Test
    void semEstabelecimentosOuCampanhasNaoHaPendencias() {
        when(acesso.estabelecimentosDo(PARCEIRO)).thenReturn(List.of());
        assertThat(servico.pendencias(PARCEIRO)).isEmpty();

        when(acesso.estabelecimentosDo(PARCEIRO)).thenReturn(List.of(5L));
        when(estabelecimentos.findByIdInOrderByNomeFantasia(List.of(5L))).thenReturn(List.of(hotel));
        when(campanhas.findByStatusDaRodada(StatusRodada.ABERTA)).thenReturn(List.of());
        assertThat(servico.pendencias(PARCEIRO)).isEmpty();
        verify(respostas, never()).findByCampanhaIdsComEstabelecimento(any());
    }

    @Test
    void minhasRespostasTrazemATaxaDaCidadeQuandoPublicada() {
        RodadaPesquisa rodada = Fixtures.rodada(1, "005/26", StatusRodada.ENCERRADA);
        Campanha fev = Fixtures.campanhaMensal(10, rodada, FEV);
        Campanha mar = Fixtures.campanhaMensal(11, rodada, FEV.plusMonths(1));
        when(acesso.estabelecimentosDo(PARCEIRO)).thenReturn(List.of(5L));
        when(respostas.findByEstabelecimentoIdsComCampanha(List.of(5L))).thenReturn(List.of(
                Fixtures.resposta(101, mar, hotel, "55", null), Fixtures.resposta(100, fev, hotel, "65", "400")));
        ResultadoCampanha publicado = org.mockito.Mockito.mock(ResultadoCampanha.class);
        when(publicado.getCampanhaId()).thenReturn(10L);
        when(publicado.getTaxaOcupacao()).thenReturn(new BigDecimal("61.20"));
        when(resultados.findByCampanhaIdIn(List.of(11L, 10L))).thenReturn(List.of(publicado));

        List<MinhaRespostaResponse> minhas = servico.minhas(PARCEIRO);

        assertThat(minhas).extracting(MinhaRespostaResponse::respostaId).containsExactly(101L, 100L);
        assertThat(minhas.get(0).taxaOcupacaoCidade()).isNull();
        assertThat(minhas.get(1).taxaOcupacaoCidade()).isEqualByComparingTo("61.20");
        assertThat(minhas.get(1).diariaMedia()).isEqualByComparingTo("400");
    }

    @Test
    void parceiroSemEstabelecimentosNaoTemRespostas() {
        when(acesso.estabelecimentosDo(PARCEIRO)).thenReturn(List.of());
        assertThat(servico.minhas(PARCEIRO)).isEmpty();

        when(acesso.estabelecimentosDo(PARCEIRO)).thenReturn(List.of(5L));
        when(respostas.findByEstabelecimentoIdsComCampanha(List.of(5L))).thenReturn(List.of());
        assertThat(servico.minhas(PARCEIRO)).isEmpty();
        verify(resultados, never()).findByCampanhaIdIn(any());
    }
}
