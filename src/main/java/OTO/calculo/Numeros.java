package OTO.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** arredondamento dos resultados da calculadora para gravação e exibição. */
public final class Numeros {

    private Numeros() {
    }

    public static BigDecimal duasCasas(Double valor) {
        return valor == null ? null : BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal duasCasas(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    public static Double paraDouble(BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }

    /** número com vírgula decimal, para mensagens ao usuário, por ex: 70,00 */
    public static String emPortugues(BigDecimal valor) {
        return valor == null ? "" : String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", valor);
    }
}
