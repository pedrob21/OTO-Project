package OTO.inventario;

import static OTO.Fixtures.ADMIN;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.parceiro;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.CatalogoItem;
import OTO.inventario.InventarioDtos.GrupoRequest;
import OTO.inventario.InventarioDtos.ItemRequest;

@WebMvcTest(CatalogoInventarioController.class)
@Import(SegurancaDeTeste.class)
class CatalogoInventarioControllerTest {

    private static final CatalogoGrupo GRUPO = new CatalogoGrupo(1L, CategoriaInventario.ACESSIBILIDADE, "RAMPA", "Rampa",
            false, 1, true, List.of());
    private static final CatalogoItem ITEM = new CatalogoItem(10L, "PISO", "Piso tátil", null, false, 1, true);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogoInventarioService servico;

    @Test
    void soAdminMantemOCatalogo() throws Exception {
        mvc.perform(get("/api/inventario/catalogo/completo").with(parceiro())).andExpect(status().isForbidden());
        verifyNoInteractions(servico);
    }

    @Test
    void grupos() throws Exception {
        GrupoRequest requisicao = new GrupoRequest(CategoriaInventario.ACESSIBILIDADE, null, "Rampa", false, null);
        when(servico.completo()).thenReturn(List.of(GRUPO));
        when(servico.criarGrupo(requisicao, ADMIN)).thenReturn(GRUPO);
        when(servico.alterarGrupo(1L, requisicao, ADMIN)).thenReturn(GRUPO);
        String corpo = """
                {"categoria": "ACESSIBILIDADE", "nome": "Rampa", "exclusivoSecretaria": false}
                """;

        mvc.perform(get("/api/inventario/catalogo/completo").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("RAMPA"));
        mvc.perform(post("/api/inventario/grupos").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/inventario/grupos/1").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/inventario/grupos/1/inativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(post("/api/inventario/grupos/1/reativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/inventario/grupos/1").with(admin())).andExpect(status().isNoContent());

        verify(servico).definirAtivoGrupo(1L, false, ADMIN);
        verify(servico).definirAtivoGrupo(1L, true, ADMIN);
        verify(servico).excluirGrupo(1L, ADMIN);
    }

    @Test
    void grupoSemCategoriaDa400() throws Exception {
        mvc.perform(post("/api/inventario/grupos").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Rampa\", \"exclusivoSecretaria\": false, \"ordem\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.categoria").exists())
                .andExpect(jsonPath("$.campos.ordem").exists());
    }

    /**
     * No Jackson 3 (Spring Boot 4), campo boolean primitivo ausente no corpo é recusado
     * (FAIL_ON_NULL_FOR_PRIMITIVES): o front precisa mandar exclusivoSecretaria, permiteComplemento etc.
     */
    @Test
    void booleanAusenteNoCorpoDa400() throws Exception {
        mvc.perform(post("/api/inventario/grupos").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\": \"ACESSIBILIDADE\", \"nome\": \"Rampa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
        verifyNoInteractions(servico);
    }

    @Test
    void itens() throws Exception {
        ItemRequest requisicao = new ItemRequest(null, "Piso tátil", null, false, null);
        when(servico.criarItem(1L, requisicao, ADMIN)).thenReturn(ITEM);
        when(servico.alterarItem(10L, requisicao, ADMIN)).thenReturn(ITEM);
        String corpo = "{\"nome\": \"Piso tátil\", \"permiteComplemento\": false}";

        mvc.perform(post("/api/inventario/grupos/1/itens").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").value("PISO"));
        mvc.perform(put("/api/inventario/itens/10").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/inventario/itens/10/inativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(post("/api/inventario/itens/10/reativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/inventario/itens/10").with(admin())).andExpect(status().isNoContent());

        verify(servico).definirAtivoItem(10L, false, ADMIN);
        verify(servico).definirAtivoItem(10L, true, ADMIN);
        verify(servico).excluirItem(10L, ADMIN);
    }
}
