package OTO.comum.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigTest {

    @Test
    void documentaAApiComAutenticacaoBearer() {
        OpenAPI api = new OpenApiConfig().openApi();
        assertThat(api.getInfo().getTitle()).contains("Observatório de Turismo de Olímpia");
        SecurityScheme token = api.getComponents().getSecuritySchemes().get("token");
        assertThat(token.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(token.getScheme()).isEqualTo("bearer");
        assertThat(api.getSecurity()).hasSize(1);
        assertThat(api.getSecurity().get(0)).containsKey("token");
    }
}
