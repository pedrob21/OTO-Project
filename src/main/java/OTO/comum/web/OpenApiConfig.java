package OTO.comum.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** documentação da API em /swagger-ui.html. Use o botão "Authorize" com o token do login. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API do Observatório de Turismo de Olímpia")
                        .version("v1")
                        .description("Pesquisas de ocupação com os parceiros, indicadores e dashboards do OTO."))
                .components(new Components().addSecuritySchemes("token",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
