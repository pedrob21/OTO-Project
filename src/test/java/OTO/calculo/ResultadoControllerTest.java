package OTO.calculo;

import static OTO.Fixtures.ADMIN;
import static OTO.Fixtures.VISUALIZADOR;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.visualizador;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.calculo.ResultadoDtos.CampanhaPublicadaResponse;
import OTO.calculo.ResultadoDtos.FaixaRef;
import OTO.calculo.ResultadoDtos.PreviaResponse;
import OTO.calculo.ResultadoDtos.ResultadoResponse;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.TipoPeriodo;

@WebMvcTest(ResultadoController.class)
@Import(SegurancaDeTeste.class)
class ResultadoControllerTest {

    private static final CampanhaResponse CAMPANHA = new CampanhaResponse(10L, 1L, "Fevereiro", Modalidade.CONSOLIDADA,
            TipoPeriodo.MENSAL, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), 28, true, null);
    private static final ResultadoResponse RESULTADO = new ResultadoResponse(CAMPANHA, true, new BigDecimal("60.00"),
            new FaixaRef("BOA", "Boa"), 3, 2, null, 117, 80, null, null, null, null, null, null, null, null, null, 1L,
            null, List.of(), List.of());

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ResultadoService servico;

    @Test
    void previaEPublicacaoSoParaOAdmin() throws Exception {
        mvc.perform(get("/api/campanhas/10/previa").with(visualizador())).andExpect(status().isForbidden());
        mvc.perform(post("/api/campanhas/10/publicacao").with(visualizador())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/campanhas/10/publicacao").with(visualizador())).andExpect(status().isForbidden());
        verifyNoInteractions(servico);
    }

    @Test
    void previaPublicarECancelar() throws Exception {
        when(servico.previa(10L)).thenReturn(new PreviaResponse(10L, 3, 2, 117, 80, new BigDecimal("68.38"),
                new BigDecimal("30.00"), true, List.of(), List.of("aviso"), RESULTADO));
        when(servico.publicar(10L, ADMIN)).thenReturn(RESULTADO);

        mvc.perform(get("/api/campanhas/10/previa").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.podePublicar").value(true))
                .andExpect(jsonPath("$.avisos[0]").value("aviso"));
        mvc.perform(post("/api/campanhas/10/publicacao").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faixa.codigo").value("BOA"));
        mvc.perform(delete("/api/campanhas/10/publicacao").with(admin())).andExpect(status().isNoContent());
        verify(servico).cancelarPublicacao(10L, ADMIN);
    }

    @Test
    void resultadoParaQualquerPerfil() throws Exception {
        when(servico.resultado(10L, VISUALIZADOR)).thenReturn(RESULTADO);
        mvc.perform(get("/api/campanhas/10/resultado").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxaOcupacao").value(60.0))
                .andExpect(jsonPath("$.campanha.nome").value("Fevereiro"));
    }

    @Test
    void csvParaDownload() throws Exception {
        byte[] csv = "﻿Campanha;Fevereiro\r\n".getBytes(StandardCharsets.UTF_8);
        when(servico.csv(10L, VISUALIZADOR)).thenReturn(csv);

        mvc.perform(get("/api/campanhas/10/resultado.csv").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"resultado-campanha-10.csv\""))
                .andExpect(content().bytes(csv));
    }

    @Test
    void publicadasComFiltros() throws Exception {
        when(servico.publicadas(Modalidade.EXPECTATIVA, TipoPeriodo.FERIADO, 2026)).thenReturn(List.of(
                new CampanhaPublicadaResponse(11L, "Carnaval", Modalidade.EXPECTATIVA, TipoPeriodo.FERIADO, "CARNAVAL",
                        null, null, null, new BigDecimal("80.00"), null, null, null)));

        mvc.perform(get("/api/campanhas/publicadas").param("modalidade", "EXPECTATIVA").param("tipoPeriodo", "FERIADO")
                        .param("ano", "2026").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].feriadoCodigo").value("CARNAVAL"));
    }
}
