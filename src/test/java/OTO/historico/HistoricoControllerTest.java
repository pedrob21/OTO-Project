package OTO.historico;

import static OTO.Fixtures.ADMIN;
import static OTO.SegurancaDeTeste.admin;
import static OTO.SegurancaDeTeste.parceiro;
import static OTO.SegurancaDeTeste.visualizador;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.SegurancaDeTeste;
import OTO.historico.HistoricoDtos.ErroLinha;
import OTO.historico.HistoricoDtos.HistoricoRequest;
import OTO.historico.HistoricoDtos.HistoricoResponse;
import OTO.historico.HistoricoDtos.ImportacaoResponse;
import OTO.pesquisa.Modalidade;

@WebMvcTest(HistoricoController.class)
@Import(SegurancaDeTeste.class)
class HistoricoControllerTest {

    private static final HistoricoResponse VALOR = new HistoricoResponse(4L, Indicador.TAXA_OCUPACAO,
            Modalidade.CONSOLIDADA, 2024, 1, null, null, new BigDecimal("63.45"), null, "Planilha");
    private static final String CORPO = """
            {"indicador": "TAXA_OCUPACAO", "modalidade": "CONSOLIDADA", "ano": 2024, "mes": 1, "valor": 63.45,
             "fonte": "Planilha"}
            """;
    private static final MockMultipartFile ARQUIVO = new MockMultipartFile("arquivo", "historico.csv", "text/csv",
            "TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45;".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HistoricoService servico;

    @Test
    void listarComFiltros() throws Exception {
        when(servico.listar(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, "HOTEL")).thenReturn(List.of(VALOR));
        mvc.perform(get("/api/historico").param("indicador", "TAXA_OCUPACAO").param("modalidade", "CONSOLIDADA")
                        .param("ano", "2024").param("tipo", "HOTEL").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value(63.45));
    }

    @Test
    void gravarCorrigirEExcluir() throws Exception {
        HistoricoRequest requisicao = new HistoricoRequest(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null,
                null, new BigDecimal("63.45"), null, "Planilha");
        when(servico.salvar(requisicao, ADMIN)).thenReturn(VALOR);
        when(servico.alterar(4L, requisicao, ADMIN)).thenReturn(VALOR);

        mvc.perform(post("/api/historico").with(parceiro()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/historico").with(admin()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
        mvc.perform(put("/api/historico/4").with(admin()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/historico/4").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluir(4L, ADMIN);
    }

    @Test
    void anoForaDoIntervaloDa400() throws Exception {
        mvc.perform(post("/api/historico").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO.replace("2024", "1999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.ano").exists());
    }

    @Test
    void importacaoSemErrosDa200() throws Exception {
        when(servico.importar(any(), eq("Planilha"), eq(ADMIN))).thenReturn(new ImportacaoResponse(1, 1, 0, List.of()));

        mvc.perform(multipart("/api/historico/importacao").file(ARQUIVO).param("fonte", "Planilha").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inseridos").value(1));
    }

    @Test
    void importacaoComErrosDa422() throws Exception {
        when(servico.importar(any(), eq("Planilha"), eq(ADMIN)))
                .thenReturn(new ImportacaoResponse(1, 0, 0, List.of(new ErroLinha(1, "valor vazio"))));

        mvc.perform(multipart("/api/historico/importacao").file(ARQUIVO).param("fonte", "Planilha").with(admin()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erros[0].mensagem").value("valor vazio"));
    }

    @Test
    void importacaoSoParaOAdmin() throws Exception {
        mvc.perform(multipart("/api/historico/importacao").file(ARQUIVO).param("fonte", "Planilha").with(visualizador()))
                .andExpect(status().isForbidden());
    }
}
