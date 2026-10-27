package OTO.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.auth.AuthDtos.DefinirSenhaRequest;
import OTO.auth.AuthDtos.LoginRequest;
import OTO.auth.AuthDtos.LoginResponse;
import OTO.comum.excecao.CredenciaisInvalidasException;
import OTO.usuario.Perfil;
import OTO.usuario.UsuarioResponse;

@WebMvcTest(AuthController.class)
@Import(SegurancaDeTeste.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService auth;

    @Test
    void loginEhPublico() throws Exception {
        UsuarioResponse usuario = new UsuarioResponse(5L, "Ana", "ana@oto.com", Perfil.ADMIN, true, false, null, List.of());
        when(auth.login(new LoginRequest("ana@oto.com", "segredo123")))
                .thenReturn(new LoginResponse("jwt", Instant.parse("2026-10-26T20:00:00Z"), usuario));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@oto.com", "senha": "segredo123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"));
    }

    @Test
    void loginComCorpoInvalidoListaOsCampos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email", "senha": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
        verify(auth, never()).login(any());
    }

    @Test
    void credenciaisErradasDao401() throws Exception {
        when(auth.login(any())).thenThrow(new CredenciaisInvalidasException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@oto.com", "senha": "errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void jsonMalformadoDa400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void definirSenhaEhPublico() throws Exception {
        mvc.perform(post("/api/auth/definir-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "tk", "novaSenha": "novaSenha1"}
                                """))
                .andExpect(status().isNoContent());
        verify(auth).definirSenha(new DefinirSenhaRequest("tk", "novaSenha1"));
    }

    @Test
    void senhaCurtaEhRecusada() throws Exception {
        mvc.perform(post("/api/auth/definir-senha").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "tk", "novaSenha": "curta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.novaSenha").value("deve ter entre 8 e 72 caracteres"));
    }
}
