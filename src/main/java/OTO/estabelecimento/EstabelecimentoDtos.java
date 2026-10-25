package OTO.estabelecimento;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Corpos de requisição e resposta de estabelecimentos, capacidade e RHC. */
public final class EstabelecimentoDtos {

    private EstabelecimentoDtos() {
    }

    /** Novo meio de hospedagem (ADMIN), já com a capacidade inicial. */
    public record CriacaoRequest(
            @NotNull @Valid DadosCadastrais dados,
            @NotNull @Valid DadosSecretaria secretaria,
            @NotNull @Valid CapacidadeRequest capacidade) {
    }

    /** Alteração. O bloco "secretaria" só é considerado quando quem altera é ADMIN. */
    public record AtualizacaoRequest(
            @NotNull @Valid DadosCadastrais dados,
            @Valid DadosSecretaria secretaria) {
    }

    public record EstabelecimentoResponse(
            Long id,
            boolean ativo,
            DadosCadastrais dados,
            DadosSecretaria secretaria,
            CapacidadeResponse capacidadeVigente,
            Instant inventarioAtualizadoEm,
            Instant criadoEm,
            Instant atualizadoEm) {

        public static EstabelecimentoResponse de(Estabelecimento e, CapacidadeHospedagem capacidade) {
            return new EstabelecimentoResponse(e.getId(), e.isAtivo(), e.getDadosCadastrais(), e.getDadosSecretaria(),
                    capacidade == null ? null : CapacidadeResponse.de(capacidade),
                    e.getInventarioAtualizadoEm(), e.getCriadoEm(), e.getAtualizadoEm());
        }
    }

    /** Linha das listagens. UH e leitos são os da capacidade vigente hoje. */
    public record EstabelecimentoResumo(
            Long id,
            String nomeFantasia,
            String tipoHospedagemCodigo,
            Situacao situacao,
            boolean ativo,
            Integer qtdUh,
            Integer qtdLeitos) {

        public static EstabelecimentoResumo de(Estabelecimento e, CapacidadeHospedagem capacidade) {
            return new EstabelecimentoResumo(e.getId(), e.getNomeFantasia(), e.getTipoHospedagemCodigo(),
                    e.getSituacao(), e.isAtivo(),
                    capacidade == null ? null : capacidade.getQtdUh(),
                    capacidade == null ? null : capacidade.getQtdLeitos());
        }
    }

    /** Nova capacidade a partir de uma data. Não altera as anteriores. */
    public record CapacidadeRequest(
            @NotNull LocalDate vigenteDesde,
            @NotNull @Positive Integer qtdUh,
            @NotNull @PositiveOrZero Integer qtdUhPcd,
            @NotNull @Positive Integer qtdLeitos,
            @NotNull @Positive @Max(30) Integer maxLeitosPorUh) {
    }

    public record CapacidadeResponse(
            Long id,
            LocalDate vigenteDesde,
            int qtdUh,
            int qtdUhPcd,
            int qtdLeitos,
            int maxLeitosPorUh,
            double leitosUteis,
            Instant registradoEm) {

        public static CapacidadeResponse de(CapacidadeHospedagem c) {
            return new CapacidadeResponse(c.getId(), c.getVigenteDesde(), c.getQtdUh(), c.getQtdUhPcd(),
                    c.getQtdLeitos(), c.getMaxLeitosPorUh(), c.getLeitosUteis(), c.getRegistradoEm());
        }
    }

    public record RhcRequest(
            @NotNull LocalDate vigenteDesde,
            @NotNull @PositiveOrZero Integer qtdRegistros,
            @NotNull @PositiveOrZero Integer qtdUh,
            @NotNull @PositiveOrZero Integer qtdLeitos,
            @NotNull @Positive @Max(30) Integer maxLeitosPorUh) {
    }

    public record RhcResponse(
            Long id,
            LocalDate vigenteDesde,
            int qtdRegistros,
            int qtdUh,
            int qtdLeitos,
            int maxLeitosPorUh,
            double leitosUteis,
            Instant registradoEm) {

        public static RhcResponse de(RhcResumo r) {
            return new RhcResponse(r.getId(), r.getVigenteDesde(), r.getQtdRegistros(), r.getQtdUh(), r.getQtdLeitos(),
                    r.getMaxLeitosPorUh(), r.getLeitosUteis(), r.getRegistradoEm());
        }
    }
}
