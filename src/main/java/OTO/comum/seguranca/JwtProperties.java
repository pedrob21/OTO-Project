package OTO.comum.seguranca;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * configuração do token de acesso.
 *
 * @param segredo chave HMAC (variável JWT_SEGREDO, com pelo menos 32 caracteres)
 * @param expiracao validade do token (ex.: 8h)
 */
@ConfigurationProperties("oto.jwt")
public record JwtProperties(String segredo, Duration expiracao) {
}
