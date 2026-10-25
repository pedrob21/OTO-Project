package OTO.pesquisa;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dados de uma campanha. Para MENSAL, informe o primeiro e o último dia do mês;
 * para FERIADO, informe também o código do feriado (ver /api/referencias).
 */
public record DadosCampanha(
        @NotBlank @Size(max = 150) String nome,
        @NotNull Modalidade modalidade,
        @NotNull TipoPeriodo tipoPeriodo,
        @Size(max = 40) String feriadoCodigo,
        @NotNull LocalDate periodoInicio,
        @NotNull LocalDate periodoFim,
        @NotNull @Positive @Max(366) Integer qtdDiarias) {
}
