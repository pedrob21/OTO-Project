package OTO.inventario;

import static OTO.Fixtures.PARCEIRO;
import static OTO.Fixtures.VISUALIZADOR;
import static OTO.SegurancaDeTeste.parceiro;
import static OTO.SegurancaDeTeste.visualizador;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import OTO.inventario.InventarioDtos.InventarioRequest;
import OTO.inventario.InventarioDtos.InventarioResponse;
import OTO.inventario.InventarioDtos.ItemMarcado;

@WebMvcTest(InventarioController.class)
@Import(SegurancaDeTeste.class)
class InventarioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InventarioService servico;

    @Test
    void catalogoParaQualquerPerfil() throws Exception {
        when(servico.catalogo()).thenReturn(List.of(new CatalogoGrupo(1L, CategoriaInventario.SERVICOS_EQUIPAMENTOS,
                "LAZER", "Lazer", false, 1, true,
                List.of(new CatalogoItem(10L, "PISCINA", "Piscina", null, false, 1, true)))));

        mvc.perform(get("/api/inventario/catalogo").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itens[0].codigo").value("PISCINA"));
    }

    @Test
    void buscarESalvarOInventario() throws Exception {
        InventarioResponse resposta = new InventarioResponse(5L, null, List.of(new ItemMarcado(11L, "Quadra")));
        when(servico.buscar(5L, VISUALIZADOR)).thenReturn(resposta);
        when(servico.salvar(5L, new InventarioRequest(List.of(new ItemMarcado(11L, "Quadra"))), PARCEIRO))
                .thenReturn(resposta);

        mvc.perform(get("/api/estabelecimentos/5/inventario").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].complemento").value("Quadra"));
        mvc.perform(put("/api/estabelecimentos/5/inventario").with(parceiro()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens": [{"itemId": 11, "complemento": "Quadra"}]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void visualizadorNaoSalva() throws Exception {
        mvc.perform(put("/api/estabelecimentos/5/inventario").with(visualizador()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itens\": []}")).andExpect(status().isForbidden());
        verify(servico, never()).salvar(any(), any(), any());
    }

    @Test
    void itemSemIdDa400() throws Exception {
        mvc.perform(put("/api/estabelecimentos/5/inventario").with(parceiro()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens": [{"complemento": "x"}]}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/estabelecimentos/5/inventario").with(parceiro()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.itens").exists());
        verify(servico, never()).salvar(any(), any(), any());
    }
}
