package OTO.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TokenUsuarioTest {

    @Test
    void valeAteExpirarOuSerUsado() {
        Instant agora = Instant.now();
        TokenUsuario token = new TokenUsuario(1L, TipoToken.CONVITE, "hash", agora.plusSeconds(60));

        assertThat(token.isValido(agora)).isTrue();
        assertThat(token.isValido(agora.plusSeconds(60))).isFalse();

        token.marcarUsado();
        assertThat(token.getUsadoEm()).isNotNull();
        assertThat(token.isValido(agora)).isFalse();
    }
}
