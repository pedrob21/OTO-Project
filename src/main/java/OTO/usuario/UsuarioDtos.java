package OTO.usuario;

import java.util.List;

import OTO.auth.LinkAcesso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpos de requisição e resposta de usuários. */
public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    /** Novo usuário. Para PARCEIRO, informe os estabelecimentos que ele representa. */
    public record UsuarioRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotNull Perfil perfil,
            List<Long> estabelecimentoIds) {
    }

    public record UsuarioAlteracaoRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotNull Perfil perfil,
            List<Long> estabelecimentoIds) {
    }

    /** Usuário criado e o convite de primeiro acesso para o admin enviar. */
    public record UsuarioCriadoResponse(UsuarioResponse usuario, LinkAcesso convite) {
    }

    public record TrocaSenhaRequest(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String novaSenha) {
    }
}
