package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import OTO.calculo.CalculadoraOcupacao.Entrada;
import OTO.calculo.CalculadoraOcupacao.Faixa;
import OTO.calculo.CalculadoraOcupacao.HospedagemCaseira;
import OTO.calculo.CalculadoraOcupacao.MeioHospedagem;
import OTO.calculo.CalculadoraOcupacao.Parametros;
import OTO.calculo.CalculadoraOcupacao.Resultado;
import OTO.calculo.CalculadoraOcupacao.Tipo;

/** Casos de borda da calculadora (o cenário principal está em {@link CalculadoraOcupacaoTest}). */
class CalculadoraOcupacaoLimitesTest {

    private static final List<Tipo> TIPOS = List.of(new Tipo("HOTEL", true));
    private static final MeioHospedagem HOTEL = new MeioHospedagem(1, "HOTEL", 10, 30, 4, 50.0, null);

    private static Parametros dayUse(double percentual) {
        return new Parametros(percentual, 4, 2, 250.0, 105.0);
    }

    @Test
    void dayUseForaDoIntervaloEhRecusado() {
        assertThatThrownBy(() -> CalculadoraOcupacao.calcular(
                new Entrada(List.of(HOTEL), null, 1, dayUse(100), TIPOS))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("day use");
        assertThatThrownBy(() -> CalculadoraOcupacao.calcular(
                new Entrada(List.of(HOTEL), null, 1, dayUse(-1), TIPOS))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void semDayUseOFluxoEhSoDosHospedados() {
        Resultado r = CalculadoraOcupacao.calcular(new Entrada(List.of(HOTEL), HospedagemCaseira.NENHUMA, 1, dayUse(0), TIPOS));
        assertThat(r.fluxoDayUse()).isZero();
        // 25 leitos úteis x 50%
        assertThat(r.fluxoTotal()).isEqualTo(12.5);
    }

    @Test
    void respondentesSemUhNaoTemComoPonderar() {
        MeioHospedagem semUh = new MeioHospedagem(1, "HOTEL", 0, 0, 1, 50.0, null);
        assertThatThrownBy(() -> CalculadoraOcupacao.calcular(new Entrada(List.of(semUh), null, 1, dayUse(0), TIPOS)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UH");
    }

    @Test
    void semDiariasNasRespostasNaoHaReceitaDeHospedagem() {
        Resultado r = CalculadoraOcupacao.calcular(new Entrada(List.of(HOTEL), null, 1, dayUse(0), TIPOS));
        assertThat(r.diariaMedia()).isNull();
        assertThat(r.receitaHospedagem()).isNull();
        assertThat(r.porTipo().get(0).revpar()).isNull();
    }

    @Test
    void diariasInformadasNulasViramMapaVazio() {
        Entrada entrada = new Entrada(List.of(HOTEL), null, 1, dayUse(0), TIPOS, null, null);
        assertThat(entrada.diariasInformadas()).isEmpty();
    }

    @Test
    void diariasInformadasSaoCopiadas() {
        HashMap<String, Double> mapa = new HashMap<>();
        mapa.put("HOTEL", 300.0);
        Entrada entrada = new Entrada(List.of(HOTEL), null, 1, dayUse(0), TIPOS, mapa, null);
        mapa.put("HOTEL", 999.0);
        assertThat(entrada.diariasInformadas()).containsEntry("HOTEL", 300.0);
    }

    @Test
    void taxaAbaixoDeTodasAsFaixasNaoTemFaixa() {
        List<Faixa> faixas = List.of(new Faixa("BAIXA", 10), new Faixa("ALTA", 50));
        assertThat(CalculadoraOcupacao.faixaDe(5, faixas)).isNull();
        assertThat(CalculadoraOcupacao.faixaDe(50, faixas).codigo()).isEqualTo("ALTA");
        assertThat(CalculadoraOcupacao.distribuirPorFaixa(
                List.of(new MeioHospedagem(1, "HOTEL", 1, 1, 1, 5.0, null)), faixas))
                .containsEntry("BAIXA", 0).containsEntry("ALTA", 0);
    }

    @Test
    void leitosUteis() {
        assertThat(HOTEL.leitosUteis()).isEqualTo(25.0);
        assertThat(new HospedagemCaseira(4, 12, 3).leitosUteis()).isEqualTo(8.0);
        assertThat(HospedagemCaseira.NENHUMA.leitosUteis()).isZero();
    }
}
