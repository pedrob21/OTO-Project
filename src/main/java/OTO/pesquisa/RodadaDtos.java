package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpos de requisição e resposta das pesquisas (rodadas) e campanhas. */
public final class RodadaDtos {

    private RodadaDtos() {
    }

    /** Nova pesquisa, criada em rascunho. As datas podem ficar para a abertura. */
    public record RodadaRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 200) String titulo,
            LocalDate dataAbertura,
            LocalDate prazoResposta) {
    }

    /** Abertura para respostas. Sem data de abertura, vale hoje. */
    public record AberturaRequest(LocalDate dataAbertura, @NotNull LocalDate prazoResposta) {
    }

    /** Novo prazo de resposta (prorrogação ou reabertura). */
    public record PrazoRequest(@NotNull LocalDate prazoResposta) {
    }

    public record CampanhaResponse(
            Long id,
            Long rodadaId,
            String nome,
            Modalidade modalidade,
            TipoPeriodo tipoPeriodo,
            String feriadoCodigo,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            int qtdDiarias,
            boolean publicada,
            Instant publicadaEm) {

        public static CampanhaResponse de(Campanha c) {
            return new CampanhaResponse(c.getId(), c.getRodada().getId(), c.getNome(), c.getModalidade(),
                    c.getTipoPeriodo(), c.getFeriadoCodigo(), c.getPeriodoInicio(), c.getPeriodoFim(),
                    c.getQtdDiarias(), c.isPublicada(), c.getPublicadaEm());
        }
    }

    public record RodadaResponse(
            Long id,
            String codigo,
            String titulo,
            StatusRodada status,
            LocalDate dataAbertura,
            LocalDate prazoResposta,
            Instant encerradaEm,
            Instant criadoEm,
            List<CampanhaResponse> campanhas) {

        public static RodadaResponse de(RodadaPesquisa r, List<Campanha> campanhas) {
            return new RodadaResponse(r.getId(), r.getCodigo(), r.getTitulo(), r.getStatus(), r.getDataAbertura(),
                    r.getPrazoResposta(), r.getEncerradaEm(), r.getCriadoEm(),
                    campanhas.stream().map(CampanhaResponse::de).toList());
        }
    }

    /** Quem já respondeu e quem falta, por campanha, para o OTO cobrar os parceiros. */
    public record AcompanhamentoResponse(
            Long rodadaId,
            String codigo,
            String titulo,
            StatusRodada status,
            LocalDate prazoResposta,
            List<AcompanhamentoCampanha> campanhas) {
    }

    public record AcompanhamentoCampanha(
            Long campanhaId,
            String nome,
            int qtdMh,
            int qtdRespondentes,
            int leitosTotal,
            int leitosRespondentes,
            BigDecimal percentualLeitosRespondentes,
            BigDecimal percentualMinimoPublicacao,
            boolean atingiuMinimo,
            List<SituacaoEstabelecimento> estabelecimentos) {
    }

    /**
     * Situação de um meio de hospedagem na campanha.
     *
     * @param semCapacidade não tem UH/leitos vigentes no período: fica fora do cálculo até ser cadastrada
     */
    public record SituacaoEstabelecimento(
            Long estabelecimentoId,
            String nomeFantasia,
            String tipoHospedagemCodigo,
            Integer qtdLeitos,
            boolean aceitaPesquisas,
            String contatoNome,
            String telefonePesquisas,
            String emailPesquisas,
            boolean respondeu,
            Integer versao,
            Instant enviadoEm,
            boolean semCapacidade) {
    }
}
