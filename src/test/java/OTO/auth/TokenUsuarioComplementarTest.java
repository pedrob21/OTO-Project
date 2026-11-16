package OTO.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;

class TokenUsuarioComplementarTest {

    @Test
    void idDoTokenGravado() {
        TokenUsuario token = Fixtures.comId(new TokenUsuario(1L, TipoToken.values()[0], "hash",
                Instant.now().plusSeconds(60)), 9L);
        assertThat(token.getId()).isEqualTo(9L);
    }

    @Test
    void construtorDoJpa() {
        assertThat(new TokenUsuario().getId()).isNull();
    }
}
