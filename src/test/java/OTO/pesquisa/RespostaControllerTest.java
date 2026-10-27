package OTO.pesquisa;

import static OTO.Fixtures.PARCEIRO;
import static OTO.SegurancaDeTeste.admin;
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
import OTO.pesquisa.RespostaDtos.HistoricoRespostaResponse;
import OTO.pesquisa.RespostaDtos.RespostaRequest;
import OTO.pesquisa.RespostaDtos.RespostaResponse;

@WebMvcTest(RespostaController.class)
@Import(SegurancaDeTeste.class)
class RespostaControllerTest {

    private static final RespostaResponse RESPOSTA = new RespostaResponse(100L, 10L, 5L, "Hotel A",
            new BigDecimal("72.50"), null, 1, 2L, null);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RespostaService servico;

    @Test
    void parceiroResponde() throws Exception {
        when(servico.responder(10L, 5L, new RespostaRequest(new BigDecimal("72.50"), null), PARCEIRO)).thenReturn(RESPOSTA);

        mvc.perform(put("/api/campanhas/10/respostas/5").with(parceiro()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxaOcupacao\": 72.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1));
    }

    @Test
    void visualizadorNaoResponde() throws Exception {
        mvc.perform(put("/api/campanhas/10/respostas/5").with(visualizador()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"taxaOcupacao\": 50}")).andExpect(status().isForbidden());
        verify(servico, never()).responder(any(), any(), any(), any());
    }

    @Test
    void taxaForaDoIntervaloOuDiariaZeroDao400() throws Exception {
        mvc.perform(put("/api/campanhas/10/respostas/5").with(parceiro()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxaOcupacao\": 100.01, \"diariaMedia\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.taxaOcupacao").exists())
                .andExpect(jsonPath("$.campos.diariaMedia").exists());
    }

    @Test
    void respostasIndividuaisSoParaOAdmin() throws Exception {
        when(servico.listarDaCampanha(10L)).thenReturn(List.of(RESPOSTA));

        mvc.perform(get("/api/campanhas/10/respostas").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeFantasia").value("Hotel A"));
        mvc.perform(get("/api/campanhas/10/respostas").with(parceiro())).andExpect(status().isForbidden());
        mvc.perform(get("/api/campanhas/10/respostas").with(visualizador())).andExpect(status().isForbidden());
    }

    @Test
    void pendenciasEMinhasSoParaOParceiro() throws Exception {
        when(servico.pendencias(PARCEIRO)).thenReturn(List.of());
        when(servico.minhas(PARCEIRO)).thenReturn(List.of());

        mvc.perform(get("/api/respostas/pendencias").with(parceiro())).andExpect(status().isOk());
        mvc.perform(get("/api/respostas/minhas").with(parceiro())).andExpect(status().isOk());
        mvc.perform(get("/api/respostas/pendencias").with(admin())).andExpect(status().isForbidden());
        mvc.perform(get("/api/respostas/minhas").with(admin())).andExpect(status().isForbidden());
    }

    @Test
    void historicoDaResposta() throws Exception {
        when(servico.historico(100L, PARCEIRO)).thenReturn(List.of(
                new HistoricoRespostaResponse(2, new BigDecimal("70"), null, 2L, null),
                new HistoricoRespostaResponse(1, new BigDecimal("60"), null, 2L, null)));

        mvc.perform(get("/api/respostas/100/historico").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versao").value(2));
        mvc.perform(get("/api/respostas/100/historico").with(visualizador())).andExpect(status().isForbidden());
    }
}
