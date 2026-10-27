package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;
import OTO.estabelecimento.Estabelecimento;

class PesquisaEntidadesTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 3, 1);

    @Test
    void rodadaPassaPorRascunhoAbertaEEncerrada() {
        RodadaPesquisa r = new RodadaPesquisa("005/26", "Março", null, null, 1L);
        assertThat(r.isRascunho()).isTrue();
        assertThat(r.getCriadoEm()).isNotNull();
        assertThat(r.getCriadoPor()).isEqualTo(1L);

        r.abrir(HOJE, HOJE.plusDays(7));
        assertThat(r.isAberta()).isTrue();
        assertThat(r.getDataAbertura()).isEqualTo(HOJE);

        r.prorrogar(HOJE.plusDays(10));
        assertThat(r.getPrazoResposta()).isEqualTo(HOJE.plusDays(10));

        r.encerrar();
        assertThat(r.isEncerrada()).isTrue();
        assertThat(r.getEncerradaEm()).isNotNull();

        r.reabrir(HOJE.plusDays(20));
        assertThat(r.isAberta()).isTrue();
        assertThat(r.getEncerradaEm()).isNull();
        assertThat(r.getPrazoResposta()).isEqualTo(HOJE.plusDays(20));

        r.alterar("006/26", "Abril", HOJE, HOJE);
        assertThat(r.getCodigo()).isEqualTo("006/26");
        assertThat(r.getTitulo()).isEqualTo("Abril");
    }

    @Test
    void campanhaSoGuardaFeriadoNoTipoFeriado() {
        RodadaPesquisa r = Fixtures.rodada(1, "X", StatusRodada.RASCUNHO);
        Campanha mensal = new Campanha(r, new DadosCampanha("Mar", Modalidade.CONSOLIDADA, TipoPeriodo.MENSAL, "CARNAVAL",
                HOJE, YearMonth.from(HOJE).atEndOfMonth(), 31));
        assertThat(mensal.getFeriadoCodigo()).isNull();

        mensal.alterar(new DadosCampanha("Carnaval", Modalidade.EXPECTATIVA, TipoPeriodo.FERIADO, "CARNAVAL", HOJE,
                HOJE.plusDays(4), 4));
        assertThat(mensal.getFeriadoCodigo()).isEqualTo("CARNAVAL");
        assertThat(mensal.getQtdDiarias()).isEqualTo(4);
        assertThat(mensal.getModalidade()).isEqualTo(Modalidade.EXPECTATIVA);
    }

    @Test
    void publicarECancelarAPublicacao() {
        Campanha c = Fixtures.campanhaMensal(1, Fixtures.rodada(1, "X", StatusRodada.ENCERRADA), YearMonth.of(2026, 2));
        assertThat(c.isPublicada()).isFalse();

        c.publicar(7L);
        assertThat(c.isPublicada()).isTrue();
        assertThat(c.getPublicadaPor()).isEqualTo(7L);

        c.cancelarPublicacao();
        assertThat(c.isPublicada()).isFalse();
        assertThat(c.getPublicadaEm()).isNull();
        assertThat(c.getPublicadaPor()).isNull();
    }

    @Test
    void correcaoDaRespostaSomaVersaoEHistoricoCopiaOEnvio() {
        Campanha c = Fixtures.campanhaMensal(1, Fixtures.rodada(1, "X", StatusRodada.ABERTA), YearMonth.of(2026, 2));
        Estabelecimento e = Fixtures.estabelecimento(5, "Hotel", "HOTEL");
        RespostaOcupacao resposta = Fixtures.resposta(100, c, e, "60", null);
        assertThat(resposta.getVersao()).isEqualTo(1);

        resposta.corrigir(new BigDecimal("70"), new BigDecimal("500"), 9L);
        assertThat(resposta.getVersao()).isEqualTo(2);
        assertThat(resposta.getEnviadoPor()).isEqualTo(9L);

        RespostaOcupacaoHistorico h = RespostaOcupacaoHistorico.de(resposta);
        assertThat(h.getRespostaId()).isEqualTo(100L);
        assertThat(h.getVersao()).isEqualTo(2);
        assertThat(h.getTaxaOcupacao()).isEqualByComparingTo("70");
        assertThat(h.getDiariaMedia()).isEqualByComparingTo("500");
        assertThat(h.getEnviadoPor()).isEqualTo(9L);
        assertThat(h.getEnviadoEm()).isEqualTo(resposta.getEnviadoEm());
    }

    @Test
    void diariaSemTipoEhAGeral() {
        CampanhaDiaria geral = new CampanhaDiaria(1L, null, BigDecimal.TEN, null, 1L);
        assertThat(geral.isGeral()).isTrue();
        assertThat(new CampanhaDiaria(1L, "HOTEL", BigDecimal.TEN, null, 1L).isGeral()).isFalse();

        geral.alterar(BigDecimal.ONE, "Booking", 2L);
        assertThat(geral.getDiariaMedia()).isEqualTo(BigDecimal.ONE);
        assertThat(geral.getRegistradoPor()).isEqualTo(2L);
    }

    @Test
    void parametroCorrigido() {
        ParametroCalculo p = Fixtures.parametro(1, HOJE);
        p.alterar(HOJE.plusDays(1), BigDecimal.ONE, 5, 3, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, "Nova", 2L);
        assertThat(p.getVigenteDesde()).isEqualTo(HOJE.plusDays(1));
        assertThat(p.getDiasEmParques()).isEqualTo(3);
        assertThat(p.getFonte()).isEqualTo("Nova");
    }
}
