package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import OTO.calculo.CalculadoraOcupacao.Resultado;
import OTO.calculo.CalculadoraOcupacao.ResultadoTipo;

class ResultadoCampanhaTest {

    @Test
    void resultadoGravadoComDuasCasas() {
        Resultado r = new Resultado(60.456, 3, 2, 117, 80, 68.376, 66.666, 110.004, 9.6, 21.255, 140.859, 70.4295,
                437.5, 15750.0, 170439.39, List.of());

        ResultadoCampanha e = ResultadoCampanha.de(10L, 1L, r);

        assertThat(e.getCampanhaId()).isEqualTo(10L);
        assertThat(e.getParametroCalculoId()).isEqualTo(1L);
        assertThat(e.getTaxaOcupacao()).isEqualTo(new BigDecimal("60.46"));
        assertThat(e.getQtdMh()).isEqualTo(3);
        assertThat(e.getQtdMhRespondentes()).isEqualTo(2);
        assertThat(e.getLeitosTotal()).isEqualTo(117);
        assertThat(e.getLeitosRespondentes()).isEqualTo(80);
        assertThat(e.getPercentualLeitosRespondentes()).isEqualTo(new BigDecimal("68.38"));
        assertThat(e.getFluxoMh()).isEqualTo(new BigDecimal("110.00"));
        assertThat(e.getFluxoRhc()).isEqualTo(new BigDecimal("9.60"));
        assertThat(e.getFluxoDayUse()).isEqualTo(new BigDecimal("21.26"));
        assertThat(e.getFluxoTotal()).isEqualTo(new BigDecimal("140.86"));
        assertThat(e.getFluxoMedioDiario()).isEqualTo(new BigDecimal("70.43"));
        assertThat(e.getDiariaMedia()).isEqualTo(new BigDecimal("437.50"));
        assertThat(e.getReceitaHospedagem()).isEqualTo(new BigDecimal("15750.00"));
        assertThat(e.getReceitaSemHospedagem()).isEqualTo(new BigDecimal("170439.39"));
        assertThat(e.getCalculadoEm()).isNotNull();
    }

    @Test
    void semDiariaOuReceitaFicamNulos() {
        Resultado r = new Resultado(50, 1, 1, 10, 10, 100, 100, 1, 0, 0, 1, 1, null, null, 0, List.of());
        ResultadoCampanha e = ResultadoCampanha.de(10L, 1L, r);
        assertThat(e.getDiariaMedia()).isNull();
        assertThat(e.getReceitaHospedagem()).isNull();
    }

    @Test
    void resultadoPorTipoEFaixa() {
        ResultadoCampanhaTipo tipo = ResultadoCampanhaTipo.de(10L,
                new ResultadoTipo("HOTEL", 2, 2, 30, 60.0, 437.5, 262.5, 15750.004));
        assertThat(tipo.getTipoHospedagemCodigo()).isEqualTo("HOTEL");
        assertThat(tipo.getQtdMh()).isEqualTo(2);
        assertThat(tipo.getQtdMhRespondentes()).isEqualTo(2);
        assertThat(tipo.getQtdUh()).isEqualTo(30);
        assertThat(tipo.getTaxaOcupacao()).isEqualTo(new BigDecimal("60.00"));
        assertThat(tipo.getRevpar()).isEqualTo(new BigDecimal("262.50"));
        assertThat(tipo.getReceitaEstimada()).isEqualTo(new BigDecimal("15750.00"));

        ResultadoCampanhaTipo semRespostas = ResultadoCampanhaTipo.de(10L,
                new ResultadoTipo("POUSADA", 1, 0, 10, null, null, null, null));
        assertThat(semRespostas.getTaxaOcupacao()).isNull();
        assertThat(semRespostas.getDiariaMedia()).isNull();

        ResultadoCampanhaFaixa faixa = new ResultadoCampanhaFaixa(10L, "BOA", 3);
        assertThat(faixa.getFaixaCodigo()).isEqualTo("BOA");
        assertThat(faixa.getQtdMh()).isEqualTo(3);
    }

    @Test
    void chavesCompostasComparamPorValor() {
        assertThat(new ResultadoCampanhaTipoId(1L, "HOTEL")).isEqualTo(new ResultadoCampanhaTipoId(1L, "HOTEL"))
                .hasSameHashCodeAs(new ResultadoCampanhaTipoId(1L, "HOTEL"))
                .isNotEqualTo(new ResultadoCampanhaTipoId(2L, "HOTEL"))
                .isNotEqualTo("HOTEL");
        assertThat(new ResultadoCampanhaFaixaId(1L, "BOA")).isEqualTo(new ResultadoCampanhaFaixaId(1L, "BOA"))
                .hasSameHashCodeAs(new ResultadoCampanhaFaixaId(1L, "BOA"))
                .isNotEqualTo(new ResultadoCampanhaFaixaId(1L, "RUIM"))
                .isNotEqualTo(null);
    }
}
