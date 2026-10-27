package OTO.pesquisa;

import static OTO.Fixtures.ADMIN;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.parceiro;
import static OTO.SegurancaDeTeste.visualizador;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.pesquisa.ParametroCalculoDtos.ParametroRequest;
import OTO.pesquisa.ParametroCalculoDtos.ParametroResponse;

@WebMvcTest(ParametroCalculoController.class)
@Import(SegurancaDeTeste.class)
class ParametroCalculoControllerTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);
    private static final ParametroResponse PARAMETRO = new ParametroResponse(1L, JAN, new BigDecimal("15.15"), 4, 2,
            new BigDecimal("250.00"), new BigDecimal("105.00"), new BigDecimal("30.00"), "Pesquisa", null);
    private static final String CORPO = """
            {"vigenteDesde": "2026-01-01", "percentualDayUse": 15.15, "diasPermanenciaMedia": 4, "diasEmParques": 2,
             "gastoMedioDiario": 250.00, "gastoMedioIngresso": 105.00, "percentualMinimoLeitosPublicacao": 30.00,
             "fonte": "Pesquisa"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ParametroCalculoService servico;

    @Test
    void listarParaAdminEVisualizador() throws Exception {
        when(servico.listar()).thenReturn(List.of(PARAMETRO));
        mvc.perform(get("/api/parametros-calculo").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diasPermanenciaMedia").value(4));
        mvc.perform(get("/api/parametros-calculo").with(parceiro())).andExpect(status().isForbidden());
    }

    @Test
    void vigenteParaQualquerPerfil() throws Exception {
        when(servico.vigente(JAN)).thenReturn(PARAMETRO);
        when(servico.vigente(LocalDate.now())).thenReturn(PARAMETRO);
        mvc.perform(get("/api/parametros-calculo/vigente").param("data", "2026-01-01").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("Pesquisa"));
        mvc.perform(get("/api/parametros-calculo/vigente").with(parceiro())).andExpect(status().isOk());
    }

    @Test
    void soAdminAltera() throws Exception {
        ParametroRequest requisicao = new ParametroRequest(JAN, new BigDecimal("15.15"), 4, 2, new BigDecimal("250.00"),
                new BigDecimal("105.00"), new BigDecimal("30.00"), "Pesquisa");
        when(servico.registrar(requisicao, ADMIN)).thenReturn(PARAMETRO);
        when(servico.alterar(1L, requisicao, ADMIN)).thenReturn(PARAMETRO);

        mvc.perform(post("/api/parametros-calculo").with(visualizador()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/parametros-calculo").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPO)).andExpect(status().isCreated());
        mvc.perform(put("/api/parametros-calculo/1").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPO)).andExpect(status().isOk());
        mvc.perform(delete("/api/parametros-calculo/1").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluir(1L, ADMIN);
    }

    @Test
    void dayUseDeCemPorCentoDa400() throws Exception {
        mvc.perform(post("/api/parametros-calculo").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO.replace("15.15", "100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.percentualDayUse").exists());
    }
}
