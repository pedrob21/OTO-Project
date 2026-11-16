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
import OTO.pesquisa.Modalidade;

/** Intervalo de anos dos feriados quando só o fim, ou nenhum dos dois, é informado. */
@WebMvcTest(DashboardController.class)
@Import(SegurancaDeTeste.class)
class DashboardControllerComplementarTest {

    private static final int ANO = LocalDate.now().getYear();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService servico;

    @Test
    void feriadosSoComAnoFimVoltamTresAnos() throws Exception {
        when(servico.feriados(Modalidade.CONSOLIDADA, null, 2020, 2022))
                .thenReturn(List.of(new FeriadoSerie("CARNAVAL", "Carnaval", List.of())));
        mvc.perform(get("/api/dashboard/feriados").param("anoFim", "2022").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].feriadoCodigo").value("CARNAVAL"));
    }

    @Test
    void feriadosSemAnosSaoDosUltimosTres() throws Exception {
        when(servico.feriados(Modalidade.EXPECTATIVA, null, ANO - 2, ANO)).thenReturn(List.of());
        mvc.perform(get("/api/dashboard/feriados").param("modalidade", "EXPECTATIVA").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
