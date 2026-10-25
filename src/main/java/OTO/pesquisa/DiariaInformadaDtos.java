package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Diárias médias digitadas pela equipe do OTO para uma campanha. */
public final class DiariaInformadaDtos {

    private DiariaInformadaDtos() {
    }

    /** {@code tipoHospedagemCodigo} nulo = diária média geral. */
    public record DiariaInformada(
            @Size(max = 20) String tipoHospedagemCodigo,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal diariaMedia) {
    }

    /**
     * Lista completa das diárias digitadas: o tipo que não vier volta a usar a média das
     * respostas. Lista vazia apaga todas.
     */
    public record DiariasRequest(@NotNull @Valid List<@NotNull DiariaInformada> diarias, @Size(max = 200) String fonte) {
    }

    public record DiariaInformadaResponse(String tipoHospedagemCodigo, BigDecimal diariaMedia, String fonte,
            Long registradoPor, Instant registradoEm) {

        public static DiariaInformadaResponse de(CampanhaDiaria d) {
            return new DiariaInformadaResponse(d.getTipoHospedagemCodigo(), d.getDiariaMedia(), d.getFonte(),
                    d.getRegistradoPor(), d.getRegistradoEm());
        }
    }
}
