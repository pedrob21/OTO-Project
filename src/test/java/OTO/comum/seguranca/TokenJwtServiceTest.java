package OTO.comum.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import OTO.Fixtures;
import OTO.comum.seguranca.TokenJwtService.TokenEmitido;
import OTO.usuario.Perfil;

class TokenJwtServiceTest {

    private final SecurityConfig config = new SecurityConfig();
    private final JwtProperties propriedades = new JwtProperties(SecurityConfigTest.SEGREDO, Duration.ofHours(8));
    private final TokenJwtService servico = new TokenJwtService(config.jwtEncoder(propriedades), propriedades);
    private final JwtDecoder decoder = config.jwtDecoder(propriedades);

    @Test
    void emiteTokenComIdPerfilENome() {
        TokenEmitido emitido = servico.emitir(Fixtures.usuario(5, "Ana", "ana@oto.com", Perfil.ADMIN));

        Jwt jwt = decoder.decode(emitido.token());
        assertThat(jwt.getSubject()).isEqualTo("5");
        assertThat(jwt.getClaimAsString("perfil")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("nome")).isEqualTo("Ana");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("oto-api");
        assertThat(jwt.getExpiresAt()).isEqualTo(emitido.expiraEm().truncatedTo(ChronoUnit.SECONDS));
        assertThat(emitido.expiraEm()).isCloseTo(Instant.now().plus(Duration.ofHours(8)), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void tokenDeOutroSegredoNaoVale() {
        String token = servico.emitir(Fixtures.usuario(5, "Ana", "ana@oto.com", Perfil.ADMIN)).token();
        JwtDecoder outro = config.jwtDecoder(new JwtProperties("outro-segredo-com-mais-de-trinta-e-dois-car", Duration.ofHours(8)));
        assertThatThrownBy(() -> outro.decode(token)).isInstanceOf(JwtException.class);
    }
}
