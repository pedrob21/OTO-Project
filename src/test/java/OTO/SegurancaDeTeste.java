package OTO;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import OTO.comum.seguranca.CorsProperties;
import OTO.comum.seguranca.JwtProperties;
import OTO.comum.seguranca.SecurityConfig;
import OTO.usuario.Perfil;

/**
 * Segurança real da API nos testes de controller ({@code @WebMvcTest}): filtro JWT, {@code @PreAuthorize}
 * e CORS. Os usuários de teste são os mesmos de {@link Fixtures}: ADMIN id 1, PARCEIRO id 2, VISUALIZADOR id 3.
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class SegurancaDeTeste {

    @Bean
    @Primary
    JwtProperties jwtDeTeste() {
        return new JwtProperties("segredo-de-teste-com-mais-de-32-caracteres", Duration.ofHours(8));
    }

    @Bean
    @Primary
    CorsProperties corsDeTeste() {
        return new CorsProperties(List.of("http://localhost:*"));
    }

    public static RequestPostProcessor admin() {
        return comPerfil(1, Perfil.ADMIN);
    }

    public static RequestPostProcessor parceiro() {
        return comPerfil(2, Perfil.PARCEIRO);
    }

    public static RequestPostProcessor visualizador() {
        return comPerfil(3, Perfil.VISUALIZADOR);
    }

    private static RequestPostProcessor comPerfil(long id, Perfil perfil) {
        return jwt().jwt(j -> j.subject(String.valueOf(id)).claim("perfil", perfil.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }
}
