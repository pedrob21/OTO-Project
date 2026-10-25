package OTO.historico;

import java.math.BigDecimal;
import java.util.List;

import OTO.pesquisa.Modalidade;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Série histórica anterior ao sistema (planilha "Indicadores do Turismo"). */
public final class HistoricoDtos {

    private HistoricoDtos() {
    }

    /**
     * Um valor de um mês OU de um feriado (informe só um dos dois). Sem tipo de
     * hospedagem, o valor é o geral da cidade. Na gravação (POST), se já existir valor
     * para a mesma combinação, ele é substituído; na correção (PUT), a combinação não
     * pode ser a de outro valor.
     */
    public record HistoricoRequest(
            @NotNull Indicador indicador,
            @NotNull Modalidade modalidade,
            @NotNull @Min(2000) @Max(2100) Integer ano,
            @Min(1) @Max(12) Integer mes,
            @Size(max = 40) String feriadoCodigo,
            @Size(max = 20) String tipoHospedagemCodigo,
            @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal valor,
            @Size(max = 300) String observacao,
            @NotBlank @Size(max = 200) String fonte) {
    }

    public record HistoricoResponse(
            Long id,
            Indicador indicador,
            Modalidade modalidade,
            int ano,
            Integer mes,
            String feriadoCodigo,
            String tipoHospedagemCodigo,
            BigDecimal valor,
            String observacao,
            String fonte) {

        public static HistoricoResponse de(IndicadorHistorico h) {
            return new HistoricoResponse(h.getId(), h.getIndicador(), h.getModalidade(), h.getAno(), h.getMes(),
                    h.getFeriadoCodigo(), h.getTipoHospedagemCodigo(), h.getValor(), h.getObservacao(), h.getFonte());
        }
    }

    /** Resultado da importação. Com qualquer erro, nada é gravado. */
    public record ImportacaoResponse(int linhasLidas, int inseridos, int atualizados, List<ErroLinha> erros) {
    }

    public record ErroLinha(int linha, String mensagem) {
    }
}
