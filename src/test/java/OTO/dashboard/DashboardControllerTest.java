package OTO.dashboard;

import static OTO.SegurancaDeTeste.parceiro;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.dashboard.DashboardDtos.FeriadoSerie;
import OTO.dashboard.DashboardDtos.ResumoResponse;
import OTO.dashboard.DashboardDtos.SerieMensalResponse;
import OTO.historico.Indicador;
import OTO.pesquisa.Modalidade;

@WebMvcTest(DashboardController.class)
@Import(SegurancaDeTeste.class)
class DashboardControllerTest {

    private static final int ANO = LocalDate.now().getYear();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService servico;

    @Test
    void resumoUsaAConsolidadaPorPadrao() throws Exception {
        when(servico.resumo(Modalidade.CONSOLIDADA)).thenReturn(new ResumoResponse(Modalidade.CONSOLIDADA, null, null, null));
        mvc.perform(get("/api/dashboard/resumo").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modalidade").value("CONSOLIDADA"));
    }

    @Test
    void semAnosASerieEhDosUltimosTres() throws Exception {
        when(servico.serieMensal(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, ANO - 2, ANO))
                .thenReturn(new SerieMensalResponse(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, List.of()));
        mvc.perform(get("/api/dashboard/serie-mensal").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicador").value("TAXA_OCUPACAO"));
    }

    @Test
    void serieComParametros() throws Exception {
        when(servico.serieMensal(Indicador.FLUXO_TURISTAS, Modalidade.EXPECTATIVA, 2020, 2022))
                .thenReturn(new SerieMensalResponse(Indicador.FLUXO_TURISTAS, Modalidade.EXPECTATIVA, List.of()));
        mvc.perform(get("/api/dashboard/serie-mensal").param("indicador", "FLUXO_TURISTAS")
                        .param("modalidade", "EXPECTATIVA").param("anoInicio", "2020").param("anoFim", "2022")
                        .with(parceiro()))
                .andExpect(status().isOk());
    }

    @Test
    void feriados() throws Exception {
        when(servico.feriados(Modalidade.CONSOLIDADA, "CARNAVAL", 2024, ANO))
                .thenReturn(List.of(new FeriadoSerie("CARNAVAL", "Carnaval", List.of())));
        mvc.perform(get("/api/dashboard/feriados").param("feriado", "CARNAVAL").param("anoInicio", "2024").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Carnaval"));
    }

    @Test
    void indicadorInexistenteDa400() throws Exception {
        mvc.perform(get("/api/dashboard/serie-mensal").param("indicador", "LUCRO").with(parceiro()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isUnauthorized());
    }
}
