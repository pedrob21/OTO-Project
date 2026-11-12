package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.comum.excecao.RegraNegocioException;
import OTO.estabelecimento.CapacidadeHospedagemRepository;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.estabelecimento.RhcResumo;
import OTO.estabelecimento.RhcResumoRepository;
import OTO.estabelecimento.Situacao;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaDiaria;
import OTO.pesquisa.CampanhaDiariaRepository;
import OTO.pesquisa.ParametroCalculo;
import OTO.pesquisa.ParametroCalculoRepository;
import OTO.pesquisa.RespostaOcupacao;
import OTO.pesquisa.RespostaOcupacaoRepository;
import OTO.pesquisa.StatusRodada;
import OTO.referencia.TipoHospedagem;
import OTO.referencia.TipoHospedagemRepository;

@ExtendWith(MockitoExtension.class)
class BaseCalculoServiceTest {

    private static final YearMonth FEV = YearMonth.of(2026, 2);
    private static final LocalDate FIM = FEV.atEndOfMonth();

    @Mock
    private EstabelecimentoRepository estabelecimentos;
    @Mock
    private CapacidadeHospedagemRepository capacidades;
    @Mock
    private RespostaOcupacaoRepository respostas;
    @Mock
    private RhcResumoRepository rhc;
    @Mock
    private ParametroCalculoRepository parametros;
    @Mock
    private TipoHospedagemRepository tipos;
    @Mock
    private CampanhaDiariaRepository diarias;

    @InjectMocks
    private BaseCalculoService servico;

    private final Campanha campanha = Fixtures.campanhaMensal(10, Fixtures.rodada(1, "X", StatusRodada.ENCERRADA), FEV);

    @Test
    void separaParticipantesSemCapacidadeERespostasDesconsideradas() {
        Estabelecimento respondeu = Fixtures.estabelecimento(1, "A respondeu", "HOTEL");
        Estabelecimento semCapacidade = Fixtures.estabelecimento(2, "B sem capacidade", "HOTEL");
        Estabelecimento irregular = Fixtures.estabelecimento(3, "C irregular", "HOTEL", Situacao.IRREGULAR);
        Estabelecimento naoRespondeu = Fixtures.estabelecimento(4, "D não respondeu", "POUSADA");
        RespostaOcupacao respostaA = Fixtures.resposta(100, campanha, respondeu, "70", null);
        RespostaOcupacao respostaB = Fixtures.resposta(101, campanha, semCapacidade, "60", null);
        RespostaOcupacao respostaC = Fixtures.resposta(102, campanha, irregular, "50", null);
        RhcResumo caseira = Fixtures.rhc(1, FEV.atDay(1), 10, 4, 12, 3);
        ParametroCalculo parametro = Fixtures.parametro(1, FEV.atDay(1));
        List<TipoHospedagem> listaTipos = List.of(new TipoHospedagem("HOTEL", "Hotel", true, 1));
        List<CampanhaDiaria> listaDiarias = List.of(new CampanhaDiaria(10L, null, java.math.BigDecimal.TEN, null, 1L));

        when(capacidades.findVigentesEm(FIM)).thenReturn(List.of(
                Fixtures.capacidade(1, respondeu, FEV.atDay(1), 10, 30, 4),
                Fixtures.capacidade(3, irregular, FEV.atDay(1), 10, 30, 4),
                Fixtures.capacidade(4, naoRespondeu, FEV.atDay(1), 10, 20, 4)));
        when(respostas.findByCampanhaIdComEstabelecimento(10L)).thenReturn(List.of(respostaC, respostaA, respostaB));
        when(estabelecimentos.findAllByOrderByNomeFantasia())
                .thenReturn(List.of(respondeu, semCapacidade, irregular, naoRespondeu));
        when(rhc.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(FIM)).thenReturn(Optional.of(caseira));
        when(parametros.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(FIM)).thenReturn(Optional.of(parametro));
        when(tipos.findAllByOrderByOrdem()).thenReturn(listaTipos);
        when(diarias.findByCampanhaId(10L)).thenReturn(listaDiarias);

        BaseCalculo base = servico.montar(campanha);

        assertThat(base.dataReferencia()).isEqualTo(FIM);
        assertThat(base.participantes()).extracting(p -> p.estabelecimento().getId()).containsExactly(1L, 4L);
        assertThat(base.participantes().get(0).resposta()).isSameAs(respostaA);
        assertThat(base.participantes().get(1).respondeu()).isFalse();
        assertThat(base.semCapacidade()).containsExactly(semCapacidade);
        assertThat(base.respostasDesconsideradas()).containsExactly(respostaB, respostaC);
        assertThat(base.rhc()).isSameAs(caseira);
        assertThat(base.parametro()).isSameAs(parametro);
        assertThat(base.tipos()).isEqualTo(listaTipos);
        assertThat(base.diariasInformadas()).isEqualTo(listaDiarias);
    }

    @Test
    void semRhcABaseFicaSemHospedagemCaseira() {
        when(parametros.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(FIM))
                .thenReturn(Optional.of(Fixtures.parametro(1, FEV.atDay(1))));
        assertThat(servico.montar(campanha).rhc()).isNull();
    }

    @Test
    void semParametrosVigentesNaoHaComoCalcular() {
        when(parametros.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(FIM)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.montar(campanha)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não há parâmetros de cálculo vigentes em 2026-02-28");
    }
}
