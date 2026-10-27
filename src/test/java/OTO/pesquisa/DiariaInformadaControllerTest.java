package OTO.pesquisa;

import static OTO.Fixtures.ADMIN;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.parceiro;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.pesquisa.DiariaInformadaDtos.DiariaInformada;
import OTO.pesquisa.DiariaInformadaDtos.DiariaInformadaResponse;
import OTO.pesquisa.DiariaInformadaDtos.DiariasRequest;

@WebMvcTest(DiariaInformadaController.class)
@Import(SegurancaDeTeste.class)
class DiariaInformadaControllerTest {

    private static final List<DiariaInformadaResponse> DIARIAS = List.of(
            new DiariaInformadaResponse(null, new BigDecimal("420.00"), "Booking", 1L, null),
            new DiariaInformadaResponse("HOTEL", new BigDecimal("450.00"), "Booking", 1L, null));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DiariaInformadaService servico;

    @Test
    void soAdmin() throws Exception {
        mvc.perform(get("/api/campanhas/10/diarias").with(parceiro())).andExpect(status().isForbidden());
    }

    @Test
    void listarESalvar() throws Exception {
        when(servico.listar(10L)).thenReturn(DIARIAS);
        when(servico.salvar(10L, new DiariasRequest(List.of(new DiariaInformada(null, new BigDecimal("420.00")),
                new DiariaInformada("HOTEL", new BigDecimal("450.00"))), "Booking"), ADMIN)).thenReturn(DIARIAS);

        mvc.perform(get("/api/campanhas/10/diarias").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].tipoHospedagemCodigo").value("HOTEL"));
        mvc.perform(put("/api/campanhas/10/diarias").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diarias": [{"diariaMedia": 420.00}, {"tipoHospedagemCodigo": "HOTEL", "diariaMedia": 450.00}],
                                 "fonte": "Booking"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void diariaZeroDa400() throws Exception {
        mvc.perform(put("/api/campanhas/10/diarias").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diarias\": [{\"diariaMedia\": 0}]}"))
                .andExpect(status().isBadRequest());
        verify(servico, never()).salvar(any(), any(), any());
    }
}
