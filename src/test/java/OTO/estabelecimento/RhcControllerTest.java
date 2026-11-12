package OTO.estabelecimento;

import static OTO.Fixtures.ADMIN;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.parceiro;
import static OTO.SegurancaDeTeste.visualizador;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.estabelecimento.EstabelecimentoDtos.RhcRequest;
import OTO.estabelecimento.EstabelecimentoDtos.RhcResponse;

@WebMvcTest(RhcController.class)
@Import(SegurancaDeTeste.class)
class RhcControllerTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);
    private static final RhcResponse RHC = new RhcResponse(1L, JAN, 40, 50, 120, 3, 100.0, null);
    private static final String CORPO = """
            {"vigenteDesde": "2026-01-01", "qtdRegistros": 40, "qtdUh": 50, "qtdLeitos": 120, "maxLeitosPorUh": 3}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RhcService servico;

    @Test
    void parceiroNaoVeORhc() throws Exception {
        mvc.perform(get("/api/rhc").with(parceiro())).andExpect(status().isForbidden());
        verifyNoInteractions(servico);
    }

    @Test
    void visualizadorConsulta() throws Exception {
        when(servico.listar()).thenReturn(List.of(RHC));
        when(servico.vigente(JAN)).thenReturn(RHC);
        when(servico.vigente(LocalDate.now())).thenReturn(RHC);

        mvc.perform(get("/api/rhc").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].qtdRegistros").value(40));
        mvc.perform(get("/api/rhc/vigente").param("data", "2026-01-01").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leitosUteis").value(100.0));
        mvc.perform(get("/api/rhc/vigente").with(visualizador())).andExpect(status().isOk());
    }

    @Test
    void semVigenteDa404() throws Exception {
        when(servico.vigente(JAN)).thenThrow(new RecursoNaoEncontradoException("Não há totais de RHC vigentes."));
        mvc.perform(get("/api/rhc/vigente").param("data", "2026-01-01").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void dataInvalidaDa400() throws Exception {
        mvc.perform(get("/api/rhc/vigente").param("data", "01/01/2026").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'data'."));
    }

    @Test
    void soAdminAltera() throws Exception {
        RhcRequest requisicao = new RhcRequest(JAN, 40, 50, 120, 3);
        when(servico.registrar(requisicao, ADMIN)).thenReturn(RHC);
        when(servico.corrigir(1L, requisicao, ADMIN)).thenReturn(RHC);

        mvc.perform(post("/api/rhc").with(visualizador()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/rhc").with(admin()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/rhc/1").with(admin()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/rhc/1").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluir(1L, ADMIN);
    }
}
