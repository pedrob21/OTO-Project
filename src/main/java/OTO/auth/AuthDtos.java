package OTO.auth;

import java.time.Instant;

import OTO.usuario.UsuarioResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** corpos de requisição e resposta da autenticação. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record LoginResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
    }

    public record DefinirSenhaRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String novaSenha) {
    }
}
