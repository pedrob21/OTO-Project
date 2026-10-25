package OTO.estabelecimento;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Campos de uso exclusivo da Secretaria: só o ADMIN altera. */
public record DadosSecretaria(
        @NotBlank String tipoHospedagemCodigo,
        @NotNull Situacao situacao,
        @Size(max = 150) String contatoNome,
        boolean aceitaPesquisas,
        @Size(max = 30) String telefonePesquisas,
        @Email @Size(max = 254) String emailPesquisas,
        boolean destinoInteligente,
        boolean dadosPddt) {
}
