package OTO.estabelecimento;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResumo;

class EstabelecimentoTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);

    @Test
    void novoEstabelecimentoEhMeioDeHospedagemAtivo() {
        Estabelecimento e = new Estabelecimento(Fixtures.dados("Hotel A"), Fixtures.secretaria("HOTEL"));
        assertThat(e.getSegmento()).isEqualTo(Estabelecimento.SEGMENTO_MEIO_HOSPEDAGEM);
        assertThat(e.isAtivo()).isTrue();
        assertThat(e.getCriadoEm()).isNotNull();
        assertThat(e.getInventarioAtualizadoEm()).isNull();
    }

    @Test
    void blocosDeDadosVoltamIguaisAosGravados() {
        DadosCadastrais dados = new DadosCadastrais("Hotel A", "Hotel A Ltda", "12345678000199", JAN, "João", "5510-8",
                "Rua 1", "Centro", "Norte", "Perto do parque", new BigDecimal("-20.73"), new BigDecimal("-48.91"),
                "1733330000", "contato@a.com", "a.com", "fb/a", "@a", CadasturExigencia.OBRIGATORIO, true, "123",
                JAN.plusYears(1), 10, 2, true, LocalTime.of(14, 0), LocalTime.NOON, "Site", "Pix");
        DadosSecretaria secretaria = Fixtures.secretaria("HOTEL");
        Estabelecimento e = new Estabelecimento(dados, secretaria);

        assertThat(e.getDadosCadastrais()).isEqualTo(dados);
        assertThat(e.getDadosSecretaria()).isEqualTo(secretaria);
        assertThat(e.getCnpj()).isEqualTo("12345678000199");
        assertThat(e.isAceitaPesquisas()).isTrue();
    }

    @Test
    void soAtivoRegularEntraNoCalculo() {
        Estabelecimento regular = Fixtures.estabelecimento(1, "A", "HOTEL");
        assertThat(regular.isConsideradoNoCalculo()).isTrue();

        assertThat(Fixtures.estabelecimento(2, "B", "HOTEL", Situacao.IRREGULAR).isConsideradoNoCalculo()).isFalse();

        regular.definirAtivo(false);
        assertThat(regular.isConsideradoNoCalculo()).isFalse();
    }

    @Test
    void leitosUteisSaoAMediaEntreUmEOMaximoPorUh() {
        Estabelecimento e = Fixtures.estabelecimento(1, "A", "HOTEL");
        CapacidadeHospedagem capacidade = Fixtures.capacidade(1, e, JAN, 10, 30, 4);
        assertThat(capacidade.getLeitosUteis()).isEqualTo(25.0);

        capacidade.corrigir(JAN.plusDays(1), 20, 2, 40, 3, 9L);
        assertThat(capacidade.getLeitosUteis()).isEqualTo(40.0);
        assertThat(capacidade.getVigenteDesde()).isEqualTo(JAN.plusDays(1));
        assertThat(capacidade.getQtdUhPcd()).isEqualTo(2);
        assertThat(capacidade.getRegistradoPor()).isEqualTo(9L);

        RhcResumo rhc = Fixtures.rhc(1, JAN, 10, 4, 12, 3);
        assertThat(rhc.getLeitosUteis()).isEqualTo(8.0);
        rhc.corrigir(JAN, 10, 6, 12, 3, 9L);
        assertThat(rhc.getLeitosUteis()).isEqualTo(12.0);
    }

    @Test
    void resumoSemCapacidadeFicaSemUhELeitos() {
        EstabelecimentoResumo resumo = EstabelecimentoResumo.de(Fixtures.estabelecimento(1, "A", "HOTEL"), null);
        assertThat(resumo.qtdUh()).isNull();
        assertThat(resumo.qtdLeitos()).isNull();
        assertThat(resumo.situacao()).isEqualTo(Situacao.REGULAR);
    }
}
