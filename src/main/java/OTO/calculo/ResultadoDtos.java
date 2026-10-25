package OTO.calculo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import OTO.pesquisa.Modalidade;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.TipoPeriodo;

/** resultados das campanhas (prévia e publicados). Só dados agregados: nenhum estabelecimento aparece. */
public final class ResultadoDtos {

    private ResultadoDtos() {
    }

    public record FaixaRef(String codigo, String nome) {
    }

    /** Respondentes em uma faixa de ocupação, como no quadro da tabulação. */
    public record FaixaQuantidade(String codigo, String nome, BigDecimal percentualMinimo, int qtdMh,
            BigDecimal percentual) {
    }

    /**
     * Resultado de um tipo de hospedagem.
     *
     * @param omitido valores ocultos porque o tipo teve poucos respondentes (sigilo dos estabelecimentos)
     */
    public record ResultadoTipoResponse(
            String tipoHospedagemCodigo,
            String nome,
            int qtdMh,
            int qtdMhRespondentes,
            int qtdUh,
            BigDecimal taxaOcupacao,
            BigDecimal diariaMedia,
            BigDecimal revpar,
            BigDecimal receitaEstimada,
            boolean omitido) {
    }

    /** Indicadores de uma campanha. Taxas em percentual; fluxos em pernoites; valores em R$. */
    public record ResultadoResponse(
            CampanhaResponse campanha,
            boolean publicado,
            BigDecimal taxaOcupacao,
            FaixaRef faixa,
            int qtdMh,
            int qtdMhRespondentes,
            BigDecimal percentualMhRespondentes,
            int leitosTotal,
            int leitosRespondentes,
            BigDecimal percentualLeitosRespondentes,
            BigDecimal fluxoMh,
            BigDecimal fluxoRhc,
            BigDecimal fluxoDayUse,
            BigDecimal fluxoTotal,
            BigDecimal fluxoMedioDiario,
            BigDecimal diariaMedia,
            BigDecimal receitaHospedagem,
            BigDecimal receitaSemHospedagem,
            Long parametroCalculoId,
            Instant calculadoEm,
            List<ResultadoTipoResponse> porTipo,
            List<FaixaQuantidade> distribuicaoFaixas) {
    }

    /**
     * Simulação antes de publicar, com o que ainda impede a publicação.
     *
     * @param resultado nulo enquanto ninguém respondeu
     */
    public record PreviaResponse(
            Long campanhaId,
            int qtdMh,
            int qtdRespondentes,
            int leitosTotal,
            int leitosRespondentes,
            BigDecimal percentualLeitosRespondentes,
            BigDecimal percentualMinimoPublicacao,
            boolean podePublicar,
            List<String> impedimentos,
            List<String> avisos,
            ResultadoResponse resultado) {
    }

    /** Linha da lista de campanhas publicadas, com os indicadores principais. */
    public record CampanhaPublicadaResponse(
            Long campanhaId,
            String nome,
            Modalidade modalidade,
            TipoPeriodo tipoPeriodo,
            String feriadoCodigo,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            Instant publicadaEm,
            BigDecimal taxaOcupacao,
            FaixaRef faixa,
            BigDecimal fluxoTotal,
            BigDecimal diariaMedia) {
    }
}
