package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ParametroCalculoDtos {

    private ParametroCalculoDtos() {
    }

    /** Nova vigência dos parâmetros da metodologia. Percentuais de 0 a 100. */
    public record ParametroRequest(
            @NotNull LocalDate vigenteDesde,
            @NotNull @DecimalMin("0") @DecimalMax(value = "100", inclusive = false) @Digits(integer = 3, fraction = 2)
            BigDecimal percentualDayUse,
            @NotNull @Positive @Max(60) Integer diasPermanenciaMedia,
            @NotNull @PositiveOrZero @Max(60) Integer diasEmParques,
            @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal gastoMedioDiario,
            @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal gastoMedioIngresso,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
            BigDecimal percentualMinimoLeitosPublicacao,
            @NotBlank @Size(max = 300) String fonte) {
    }

    public record ParametroResponse(
            Long id,
            LocalDate vigenteDesde,
            BigDecimal percentualDayUse,
            int diasPermanenciaMedia,
            int diasEmParques,
            BigDecimal gastoMedioDiario,
            BigDecimal gastoMedioIngresso,
            BigDecimal percentualMinimoLeitosPublicacao,
            String fonte,
            Instant registradoEm) {

        public static ParametroResponse de(ParametroCalculo p) {
            return new ParametroResponse(p.getId(), p.getVigenteDesde(), p.getPercentualDayUse(),
                    p.getDiasPermanenciaMedia(), p.getDiasEmParques(), p.getGastoMedioDiario(),
                    p.getGastoMedioIngresso(), p.getPercentualMinimoLeitosPublicacao(), p.getFonte(),
                    p.getRegistradoEm());
        }
    }
}
