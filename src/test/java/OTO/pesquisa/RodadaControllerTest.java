package OTO.pesquisa;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.Fixtures;
import OTO.SegurancaDeTeste;
import OTO.pesquisa.RodadaDtos.AberturaRequest;
import OTO.pesquisa.RodadaDtos.AcompanhamentoResponse;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.RodadaDtos.PrazoRequest;
import OTO.pesquisa.RodadaDtos.RodadaRequest;
import OTO.pesquisa.RodadaDtos.RodadaResponse;

@WebMvcTest(RodadaController.class)
@Import(SegurancaDeTeste.class)
class RodadaControllerTest {

    private static final YearMonth FEV = YearMonth.of(2026, 2);
    private static final RodadaResponse RODADA = new RodadaResponse(7L, "005/26", "Fevereiro", StatusRodada.RASCUNHO,
            null, null, null, null, List.of());
    private static final CampanhaResponse CAMPANHA = new CampanhaResponse(10L, 7L, "Fevereiro", Modalidade.CONSOLIDADA,
            TipoPeriodo.MENSAL, null, FEV.atDay(1), FEV.atEndOfMonth(), 28, false, null);
    private static final String CAMPANHA_JSON = """
            {"nome": "Fevereiro", "modalidade": "CONSOLIDADA", "tipoPeriodo": "MENSAL",
             "periodoInicio": "2026-02-01", "periodoFim": "2026-02-28", "qtdDiarias": 28}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RodadaService servico;

    @Test
    void soAdminVeAsPesquisas() throws Exception {
        mvc.perform(get("/api/rodadas").with(parceiro())).andExpect(status().isForbidden());
        verifyNoInteractions(servico);
    }

    @Test
    void listarPorSituacao() throws Exception {
        when(servico.listar(StatusRodada.ABERTA)).thenReturn(List.of(RODADA));
        mvc.perform(get("/api/rodadas").param("status", "ABERTA").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("005/26"));
        mvc.perform(get("/api/rodadas").param("status", "FECHADA").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'status'."));
    }

    @Test
    void cadastro() throws Exception {
        RodadaRequest requisicao = new RodadaRequest("005/26", "Fevereiro", null, null);
        when(servico.criar(requisicao, ADMIN)).thenReturn(RODADA);
        when(servico.alterar(7L, requisicao, ADMIN)).thenReturn(RODADA);
        when(servico.buscar(7L)).thenReturn(RODADA);
        String corpo = "{\"codigo\": \"005/26\", \"titulo\": \"Fevereiro\"}";

        mvc.perform(post("/api/rodadas").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/rodadas/7"));
        mvc.perform(get("/api/rodadas/7").with(admin())).andExpect(status().isOk());
        mvc.perform(put("/api/rodadas/7").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/rodadas/7").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluir(7L, ADMIN);
    }

    @Test
    void rodadaSemCodigoDa400() throws Exception {
        mvc.perform(post("/api/rodadas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Fevereiro\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").exists());
    }

    @Test
    void campanhas() throws Exception {
        DadosCampanha dados = Fixtures.dadosMensal("Fevereiro", Modalidade.CONSOLIDADA, FEV, 28);
        when(servico.adicionarCampanha(7L, dados, ADMIN)).thenReturn(CAMPANHA);
        when(servico.alterarCampanha(7L, 10L, dados, ADMIN)).thenReturn(CAMPANHA);

        mvc.perform(post("/api/rodadas/7/campanhas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(CAMPANHA_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qtdDiarias").value(28));
        mvc.perform(put("/api/rodadas/7/campanhas/10").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(CAMPANHA_JSON)).andExpect(status().isOk());
        mvc.perform(delete("/api/rodadas/7/campanhas/10").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluirCampanha(7L, 10L, ADMIN);
    }

    @Test
    void campanhaComMaisDe366DiariasDa400() throws Exception {
        mvc.perform(post("/api/rodadas/7/campanhas").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(CAMPANHA_JSON.replace("28}", "367}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.qtdDiarias").exists());
    }

    @Test
    void cicloDeVida() throws Exception {
        LocalDate prazo = LocalDate.of(2026, 3, 10);
        when(servico.abrir(7L, new AberturaRequest(null, prazo), ADMIN)).thenReturn(RODADA);
        when(servico.prorrogar(7L, new PrazoRequest(prazo), ADMIN)).thenReturn(RODADA);
        when(servico.encerrar(7L, ADMIN)).thenReturn(RODADA);
        when(servico.reabrir(7L, new PrazoRequest(prazo), ADMIN)).thenReturn(RODADA);
        String corpo = "{\"prazoResposta\": \"2026-03-10\"}";

        mvc.perform(post("/api/rodadas/7/abrir").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/rodadas/7/prorrogar").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/rodadas/7/encerrar").with(admin())).andExpect(status().isOk());
        mvc.perform(post("/api/rodadas/7/reabrir").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        mvc.perform(post("/api/rodadas/7/abrir").with(admin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.prazoResposta").exists());
    }

    @Test
    void acompanhamento() throws Exception {
        when(servico.acompanhamento(7L)).thenReturn(new AcompanhamentoResponse(7L, "005/26", "Fevereiro",
                StatusRodada.ABERTA, null, List.of()));
        mvc.perform(get("/api/rodadas/7/acompanhamento").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }
}
