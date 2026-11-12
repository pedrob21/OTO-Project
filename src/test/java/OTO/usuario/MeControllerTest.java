package OTO.usuario;

import static OTO.SegurancaDeTeste.parceiro;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.usuario.UsuarioDtos.TrocaSenhaRequest;

@WebMvcTest(MeController.class)
@Import(SegurancaDeTeste.class)
class MeControllerTest {

    private static final UsuarioLogado LOGADO = new UsuarioLogado(2L, Perfil.PARCEIRO);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService servico;

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meusDadosSaoDoUsuarioDoToken() throws Exception {
        when(servico.meusDados(LOGADO))
                .thenReturn(new UsuarioResponse(2L, "Pedro", "p@x.com", Perfil.PARCEIRO, true, false, null, List.of()));

        mvc.perform(get("/api/me").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("p@x.com"));
    }

    @Test
    void trocarSenha() throws Exception {
        mvc.perform(put("/api/me/senha").with(parceiro()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senhaAtual": "antiga123", "novaSenha": "nova12345"}
                                """))
                .andExpect(status().isNoContent());
        verify(servico).trocarSenha(LOGADO, new TrocaSenhaRequest("antiga123", "nova12345"));
    }

    @Test
    void corsLiberaOFrontLocal() throws Exception {
        mvc.perform(options("/api/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsBloqueiaOutraOrigem() throws Exception {
        mvc.perform(options("/api/me")
                        .header(HttpHeaders.ORIGIN, "http://malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
