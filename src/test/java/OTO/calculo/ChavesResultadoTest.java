package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Chaves compostas dos resultados: o Hibernate depende de equals e hashCode para identificar as linhas. */
class ChavesResultadoTest {

    @Test
    void chaveDoTipoComparaCampanhaETipo() {
        ResultadoCampanhaTipoId hotel = new ResultadoCampanhaTipoId(10L, "HOTEL");

        assertThat(hotel.getCampanhaId()).isEqualTo(10L);
        assertThat(hotel.getTipoHospedagemCodigo()).isEqualTo("HOTEL");
        assertThat(hotel).isEqualTo(hotel);
        assertThat(hotel).isEqualTo(new ResultadoCampanhaTipoId(10L, "HOTEL"));
        assertThat(hotel).hasSameHashCodeAs(new ResultadoCampanhaTipoId(10L, "HOTEL"));
        assertThat(hotel).isNotEqualTo(new ResultadoCampanhaTipoId(11L, "HOTEL"));
        assertThat(hotel).isNotEqualTo(new ResultadoCampanhaTipoId(10L, "POUSADA"));
        assertThat(hotel).isNotEqualTo(null);
        assertThat(hotel).isNotEqualTo("HOTEL");
    }

    @Test
    void chaveDaFaixaComparaCampanhaEFaixa() {
        ResultadoCampanhaFaixaId boa = new ResultadoCampanhaFaixaId(10L, "BOA");

        assertThat(boa.getCampanhaId()).isEqualTo(10L);
        assertThat(boa.getFaixaCodigo()).isEqualTo("BOA");
        assertThat(boa).isEqualTo(boa);
        assertThat(boa).isEqualTo(new ResultadoCampanhaFaixaId(10L, "BOA"));
        assertThat(boa).hasSameHashCodeAs(new ResultadoCampanhaFaixaId(10L, "BOA"));
        assertThat(boa).isNotEqualTo(new ResultadoCampanhaFaixaId(11L, "BOA"));
        assertThat(boa).isNotEqualTo(new ResultadoCampanhaFaixaId(10L, "RUIM"));
        assertThat(boa).isNotEqualTo(null);
        assertThat(boa).isNotEqualTo("BOA");
    }

    @Test
    void construtoresDoJpaCriamChavesVazias() {
        assertThat(new ResultadoCampanhaTipoId()).isEqualTo(new ResultadoCampanhaTipoId());
        assertThat(new ResultadoCampanhaFaixaId()).isEqualTo(new ResultadoCampanhaFaixaId());
        assertThat(new ResultadoCampanhaTipoId().getCampanhaId()).isNull();
        assertThat(new ResultadoCampanhaFaixaId().getFaixaCodigo()).isNull();
    }

    @Test
    void faixaDoResultadoGuardaCodigoEQuantidade() {
        ResultadoCampanhaFaixa faixa = new ResultadoCampanhaFaixa(10L, "BOA", 3);

        assertThat(faixa.getFaixaCodigo()).isEqualTo("BOA");
        assertThat(faixa.getQtdMh()).isEqualTo(3);
        assertThat(new ResultadoCampanhaFaixa()).isNotNull();
    }
}
