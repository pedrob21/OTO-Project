package OTO.comum.seguranca;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** origens do front que podem chamar a API pelo navegador (aceita padrões como http://localhost:*). */
@ConfigurationProperties("oto.cors")
public record CorsProperties(List<String> origensPermitidas) {
}
