package OTO.referencia;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tabelas de referência: tipos de hospedagem, feriados e eventos, faixas de ocupação.
 * Nos cadastros, o código é opcional (gerado a partir do nome) e não muda depois.
 */
public final class ReferenciaDtos {

    private ReferenciaDtos() {
    }

    public record Referencias(List<TipoRef> tiposHospedagem, List<FeriadoRef> feriados, List<FaixaRef> faixasOcupacao) {
    }

    /** {@code calculaReceita}: o tipo entra na diária média, no RevPAR e na receita estimada. */
    public record TipoRef(String codigo, String nome, boolean calculaReceita, int ordem) {

        public static TipoRef de(TipoHospedagem t) {
            return new TipoRef(t.getCodigo(), t.getNome(), t.isCalculaReceita(), t.getOrdem());
        }
    }

    public record FeriadoRef(String codigo, String nome, int ordem) {

        public static FeriadoRef de(FeriadoEvento f) {
            return new FeriadoRef(f.getCodigo(), f.getNome(), f.getOrdem());
        }
    }

    /** A faixa de uma taxa é a de maior percentual mínimo que não passa da taxa. */
    public record FaixaRef(String codigo, String nome, BigDecimal percentualMinimo) {

        public static FaixaRef de(FaixaOcupacao f) {
            return new FaixaRef(f.getCodigo(), f.getNome(), f.getPercentualMinimo());
        }
    }

    public record TipoRequest(
            @Size(max = 20) String codigo,
            @NotBlank @Size(max = 60) String nome,
            boolean calculaReceita,
            @Min(1) @Max(9999) Integer ordem) {
    }

    public record FeriadoRequest(
            @Size(max = 40) String codigo,
            @NotBlank @Size(max = 100) String nome,
            @Min(1) @Max(9999) Integer ordem) {
    }

    public record FaixaRequest(
            @Size(max = 20) String codigo,
            @NotBlank @Size(max = 40) String nome,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal percentualMinimo) {
    }
}
