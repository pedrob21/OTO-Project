package OTO.comum.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import OTO.Fixtures;
import OTO.usuario.Perfil;

class SecurityConfigTest {

    static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void segredoCurtoOuAusenteImpedeASubida() {
        assertThatThrownBy(() -> config.jwtEncoder(new JwtProperties("curto", Duration.ofHours(8))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SEGREDO");
        assertThatThrownBy(() -> config.jwtDecoder(new JwtProperties(null, Duration.ofHours(8))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void perfilDoTokenViraRole() {
        List<String> roles = config.jwtAuthenticationConverter().convert(Fixtures.jwt(1, Perfil.ADMIN))
                .getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(roles).contains("ROLE_ADMIN").noneMatch(r -> r.startsWith("ROLE_") && !r.equals("ROLE_ADMIN"));
    }

    @Test
    void senhasUsamBcrypt() {
        PasswordEncoder encoder = config.passwordEncoder();
        String hash = encoder.encode("minhaSenha");
        assertThat(hash).startsWith("$2");
        assertThat(encoder.matches("minhaSenha", hash)).isTrue();
        assertThat(encoder.matches("outra", hash)).isFalse();
    }

    @Test
    void corsLiberaAsOrigensConfiguradasSoNaApi() {
        UrlBasedCorsConfigurationSource fonte = (UrlBasedCorsConfigurationSource) config
                .corsConfigurationSource(new CorsProperties(List.of("http://localhost:*")));
        CorsConfiguration cors = fonte.getCorsConfiguration(requisicao("/api/me"));
        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(cors.checkOrigin("http://malicioso.com")).isNull();
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getExposedHeaders()).containsExactly("Content-Disposition");
        assertThat(fonte.getCorsConfiguration(requisicao("/swagger-ui.html"))).isNull();
    }

    @Test
    void corsSemOrigensNaoLiberaNenhuma() {
        UrlBasedCorsConfigurationSource fonte = (UrlBasedCorsConfigurationSource) config
                .corsConfigurationSource(new CorsProperties(null));
        assertThat(fonte.getCorsConfiguration(requisicao("/api/me")).checkOrigin("http://localhost:5173")).isNull();
    }

    private static MockHttpServletRequest requisicao(String caminho) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", caminho);
        requisicao.setRequestURI(caminho);
        return requisicao;
    }
}
