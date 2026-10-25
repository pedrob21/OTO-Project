package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import OTO.estabelecimento.Estabelecimento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Respostas de ocupação enviadas pelos parceiros (ou lançadas pelo OTO). */
public final class RespostaDtos {

    private RespostaDtos() {
    }

    /**
     * @param taxaOcupacao percentual de 0 a 100, com até duas casas
     * @param diariaMedia opcional: diária de 1 apartamento standard para 2 adultos e 1 criança (R$)
     */
    public record RespostaRequest(
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal taxaOcupacao,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal diariaMedia) {
    }

    public record RespostaResponse(
            Long id,
            Long campanhaId,
            Long estabelecimentoId,
            String nomeFantasia,
            BigDecimal taxaOcupacao,
            BigDecimal diariaMedia,
            int versao,
            Long enviadoPor,
            Instant enviadoEm) {

        public static RespostaResponse de(RespostaOcupacao r) {
            Estabelecimento e = r.getEstabelecimento();
            return new RespostaResponse(r.getId(), r.getCampanha().getId(), e.getId(), e.getNomeFantasia(),
                    r.getTaxaOcupacao(), r.getDiariaMedia(), r.getVersao(), r.getEnviadoPor(), r.getEnviadoEm());
        }
    }

    public record HistoricoRespostaResponse(int versao, BigDecimal taxaOcupacao, BigDecimal diariaMedia,
            Long enviadoPor, Instant enviadoEm) {

        public static HistoricoRespostaResponse de(RespostaOcupacaoHistorico h) {
            return new HistoricoRespostaResponse(h.getVersao(), h.getTaxaOcupacao(), h.getDiariaMedia(),
                    h.getEnviadoPor(), h.getEnviadoEm());
        }
    }

    /** Uma campanha aberta para um estabelecimento do parceiro, respondida ou não. */
    public record PendenciaResponse(
            Long campanhaId,
            String campanhaNome,
            Modalidade modalidade,
            TipoPeriodo tipoPeriodo,
            String feriadoCodigo,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            String rodadaCodigo,
            LocalDate prazoResposta,
            Long estabelecimentoId,
            String nomeFantasia,
            boolean respondida,
            Long respostaId,
            BigDecimal taxaOcupacao,
            BigDecimal diariaMedia,
            Integer versao,
            Instant enviadoEm) {

        public static PendenciaResponse de(Campanha c, Estabelecimento e, RespostaOcupacao r) {
            RodadaPesquisa rodada = c.getRodada();
            return new PendenciaResponse(c.getId(), c.getNome(), c.getModalidade(), c.getTipoPeriodo(),
                    c.getFeriadoCodigo(), c.getPeriodoInicio(), c.getPeriodoFim(), rodada.getCodigo(),
                    rodada.getPrazoResposta(), e.getId(), e.getNomeFantasia(), r != null,
                    r == null ? null : r.getId(),
                    r == null ? null : r.getTaxaOcupacao(),
                    r == null ? null : r.getDiariaMedia(),
                    r == null ? null : r.getVersao(),
                    r == null ? null : r.getEnviadoEm());
        }
    }

    /**
     * Resposta do próprio parceiro ao lado da taxa geral da cidade, quando a campanha já foi publicada.
     */
    public record MinhaRespostaResponse(
            Long respostaId,
            Long campanhaId,
            String campanhaNome,
            Modalidade modalidade,
            TipoPeriodo tipoPeriodo,
            String feriadoCodigo,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            Long estabelecimentoId,
            String nomeFantasia,
            BigDecimal taxaOcupacao,
            BigDecimal diariaMedia,
            BigDecimal taxaOcupacaoCidade) {
    }
}
