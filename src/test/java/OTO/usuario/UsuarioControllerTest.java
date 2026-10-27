package OTO.usuario;

import static OTO.Fixtures.ADMIN;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.parceiro;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import OTO.auth.LinkAcesso;
import OTO.auth.TipoToken;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.usuario.UsuarioDtos.UsuarioAlteracaoRequest;
import OTO.usuario.UsuarioDtos.UsuarioCriadoResponse;
import OTO.usuario.UsuarioDtos.UsuarioRequest;

@WebMvcTest(UsuarioController.class)
@Import(SegurancaDeTeste.class)
class UsuarioControllerTest {

    private static final UsuarioResponse BIA = new UsuarioResponse(10L, "Bia", "bia@oto.com", Perfil.VISUALIZADOR, true,
            true, null, List.of());

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService servico;

    @Test
    void soAdminGerenciaUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").with(parceiro()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Seu perfil não tem acesso a este recurso."));
        verifyNoInteractions(servico);
    }

    @Test
    void listarEBuscar() throws Exception {
        when(servico.listar()).thenReturn(List.of(BIA));
        when(servico.buscar(10L)).thenReturn(BIA);

        mvc.perform(get("/api/usuarios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Bia"));
        mvc.perform(get("/api/usuarios/10").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primeiroAcessoPendente").value(true));
    }

    @Test
    void usuarioInexistenteDa404() throws Exception {
        when(servico.buscar(9L)).thenThrow(RecursoNaoEncontradoException.de("Usuário", 9L));
        mvc.perform(get("/api/usuarios/9").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário 9 não encontrado(a)."));
    }

    @Test
    void idQueNaoEhNumeroDa400() throws Exception {
        mvc.perform(get("/api/usuarios/abc").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'id'."));
    }

    @Test
    void criarDevolve201ComOConvite() throws Exception {
        UsuarioRequest requisicao = new UsuarioRequest("Bia", "bia@oto.com", Perfil.VISUALIZADOR, null);
        when(servico.criar(requisicao, ADMIN)).thenReturn(new UsuarioCriadoResponse(BIA,
                new LinkAcesso("tk", TipoToken.CONVITE, Instant.parse("2026-11-02T00:00:00Z"))));

        mvc.perform(post("/api/usuarios").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Bia", "email": "bia@oto.com", "perfil": "VISUALIZADOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/usuarios/10"))
                .andExpect(jsonPath("$.convite.token").value("tk"))
                .andExpect(jsonPath("$.convite.tipo").value("CONVITE"));
    }

    @Test
    void criarSemPerfilDa400() throws Exception {
        mvc.perform(post("/api/usuarios").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Bia", "email": "bia@oto.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.perfil").exists());
        verify(servico, never()).criar(any(), any());
    }

    @Test
    void regraDeNegocioDa422() throws Exception {
        when(servico.criar(any(), any())).thenThrow(new RegraNegocioException("Já existe um usuário com este e-mail."));
        mvc.perform(post("/api/usuarios").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Bia", "email": "bia@oto.com", "perfil": "ADMIN"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Operação não permitida"))
                .andExpect(jsonPath("$.detail").value("Já existe um usuário com este e-mail."));
    }

    @Test
    void alterar() throws Exception {
        when(servico.alterar(10L, new UsuarioAlteracaoRequest("Beatriz", Perfil.PARCEIRO, List.of(7L)), ADMIN))
                .thenReturn(BIA);
        mvc.perform(put("/api/usuarios/10").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Beatriz", "perfil": "PARCEIRO", "estabelecimentoIds": [7]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void inativarReativarEGerarLink() throws Exception {
        when(servico.gerarLinkDeSenha(10L, ADMIN))
                .thenReturn(new LinkAcesso("tk", TipoToken.REDEFINICAO_SENHA, Instant.parse("2026-10-27T00:00:00Z")));

        mvc.perform(post("/api/usuarios/10/inativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(post("/api/usuarios/10/reativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(post("/api/usuarios/10/link-de-senha").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("REDEFINICAO_SENHA"));

        verify(servico).definirAtivo(10L, false, ADMIN);
        verify(servico).definirAtivo(10L, true, ADMIN);
    }
}
