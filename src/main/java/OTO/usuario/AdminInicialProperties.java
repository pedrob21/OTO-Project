package OTO.usuario;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Primeiro administrador, criado na subida quando ainda não há nenhum (variáveis ADMIN_* do .env). */
@ConfigurationProperties("oto.admin-inicial")
public record AdminInicialProperties(String nome, String email, String senha) {
}
