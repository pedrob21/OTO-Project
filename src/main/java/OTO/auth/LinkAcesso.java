package OTO.auth;

import java.time.Instant;

/**
 * token de primeiro acesso ou de redefinição de senha, para o admin enviar ao
 * usuário (ex.: pelo WhatsApp). O front monta o link com ele, por exemplo
 * {@code https://<front>/definir-senha?token=...}, e chama POST /api/auth/definir-senha.
 */
public record LinkAcesso(String token, TipoToken tipo, Instant expiraEm) {
}
