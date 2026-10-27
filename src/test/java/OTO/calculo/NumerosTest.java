package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class NumerosTest {

    @Test
    void arredondaParaDuasCasasMeioParaCima() {
        assertThat(Numeros.duasCasas(2.345)).isEqualTo(new BigDecimal("2.35"));
        assertThat(Numeros.duasCasas(2.344)).isEqualTo(new BigDecimal("2.34"));
        assertThat(Numeros.duasCasas(Double.valueOf(10))).isEqualTo(new BigDecimal("10.00"));
        assertThat(Numeros.duasCasas((Double) null)).isNull();
    }

    @Test
    void converteParaDouble() {
        assertThat(Numeros.paraDouble(new BigDecimal("450.50"))).isEqualTo(450.5);
        assertThat(Numeros.paraDouble(null)).isNull();
    }

    @Test
    void formataComVirgulaDecimal() {
        assertThat(Numeros.emPortugues(new BigDecimal("70"))).isEqualTo("70,00");
        assertThat(Numeros.emPortugues(new BigDecimal("1234.567"))).isEqualTo("1234,57");
        assertThat(Numeros.emPortugues(null)).isEmpty();
    }
}
