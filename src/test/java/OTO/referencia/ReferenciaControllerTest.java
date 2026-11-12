package OTO.referencia;

import static OTO.Fixtures.ADMIN;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.parceiro;
import static OTO.SegurancaDeTeste.visualizador;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import OTO.referencia.ReferenciaDtos.FaixaRef;
import OTO.referencia.ReferenciaDtos.FaixaRequest;
import OTO.referencia.ReferenciaDtos.FeriadoRef;
import OTO.referencia.ReferenciaDtos.FeriadoRequest;
import OTO.referencia.ReferenciaDtos.Referencias;
import OTO.referencia.ReferenciaDtos.TipoRef;
import OTO.referencia.ReferenciaDtos.TipoRequest;

@WebMvcTest(ReferenciaController.class)
@Import(SegurancaDeTeste.class)
class ReferenciaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReferenciaService servico;

    @Test
    void listarParaQualquerPerfil() throws Exception {
        when(servico.listar()).thenReturn(new Referencias(List.of(new TipoRef("HOTEL", "Hotel", true, 1)),
                List.of(new FeriadoRef("CARNAVAL", "Carnaval", 1)),
                List.of(new FaixaRef("RUIM", "Ruim", new BigDecimal("0.00")))));

        mvc.perform(get("/api/referencias").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiposHospedagem[0].codigo").value("HOTEL"))
                .andExpect(jsonPath("$.feriados[0].nome").value("Carnaval"))
                .andExpect(jsonPath("$.faixasOcupacao[0].percentualMinimo").value(0.0));
    }

    @Test
    void tipos() throws Exception {
        TipoRequest requisicao = new TipoRequest(null, "Chalé", true, null);
        TipoRef chale = new TipoRef("CHALE", "Chalé", true, 6);
        when(servico.criarTipo(requisicao, ADMIN)).thenReturn(chale);
        when(servico.alterarTipo("CHALE", requisicao, ADMIN)).thenReturn(chale);
        String corpo = "{\"nome\": \"Chalé\", \"calculaReceita\": true}";

        mvc.perform(post("/api/referencias/tipos-hospedagem").with(visualizador()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isForbidden());
        mvc.perform(post("/api/referencias/tipos-hospedagem").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isCreated()).andExpect(jsonPath("$.ordem").value(6));
        mvc.perform(put("/api/referencias/tipos-hospedagem/CHALE").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isOk());
        mvc.perform(delete("/api/referencias/tipos-hospedagem/CHALE").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluirTipo("CHALE", ADMIN);
    }

    @Test
    void feriados() throws Exception {
        FeriadoRequest requisicao = new FeriadoRequest("PASCOA", "Páscoa", 2);
        FeriadoRef pascoa = new FeriadoRef("PASCOA", "Páscoa", 2);
        when(servico.criarFeriado(requisicao, ADMIN)).thenReturn(pascoa);
        when(servico.alterarFeriado("PASCOA", requisicao, ADMIN)).thenReturn(pascoa);
        String corpo = "{\"codigo\": \"PASCOA\", \"nome\": \"Páscoa\", \"ordem\": 2}";

        mvc.perform(post("/api/referencias/feriados").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isCreated());
        mvc.perform(put("/api/referencias/feriados/PASCOA").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isOk());
        mvc.perform(delete("/api/referencias/feriados/PASCOA").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluirFeriado("PASCOA", ADMIN);
    }

    @Test
    void faixas() throws Exception {
        FaixaRequest requisicao = new FaixaRequest(null, "Boa", new BigDecimal("41"));
        FaixaRef boa = new FaixaRef("BOA", "Boa", new BigDecimal("41.00"));
        when(servico.criarFaixa(requisicao, ADMIN)).thenReturn(boa);
        when(servico.alterarFaixa("BOA", requisicao, ADMIN)).thenReturn(boa);
        String corpo = "{\"nome\": \"Boa\", \"percentualMinimo\": 41}";

        mvc.perform(post("/api/referencias/faixas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isCreated());
        mvc.perform(put("/api/referencias/faixas/BOA").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isOk());
        mvc.perform(delete("/api/referencias/faixas/BOA").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluirFaixa("BOA", ADMIN);
    }

    @Test
    void faixaAcimaDeCemDa400() throws Exception {
        mvc.perform(post("/api/referencias/faixas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Demais\", \"percentualMinimo\": 101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.percentualMinimo").exists());
        verify(servico, never()).criarFaixa(any(), any());
    }
}
