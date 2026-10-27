package OTO.estabelecimento;

import static OTO.Fixtures.ADMIN;
import static OTO.Fixtures.PARCEIRO;
import static OTO.Fixtures.VISUALIZADOR;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import OTO.Fixtures;
import OTO.SegurancaDeTeste;
import OTO.estabelecimento.EstabelecimentoDtos.AtualizacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeResponse;
import OTO.estabelecimento.EstabelecimentoDtos.CriacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResponse;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResumo;

@WebMvcTest(EstabelecimentoController.class)
@Import(SegurancaDeTeste.class)
class EstabelecimentoControllerTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);
    private static final EstabelecimentoResponse HOTEL = new EstabelecimentoResponse(10L, true, Fixtures.dados("Hotel A"),
            Fixtures.secretaria("HOTEL"), null, null, null, null);
    private static final CapacidadeResponse CAPACIDADE = new CapacidadeResponse(5L, JAN, 10, 1, 30, 4, 25.0, null);
    private static final String CAPACIDADE_JSON = """
            {"vigenteDesde": "2026-01-01", "qtdUh": 10, "qtdUhPcd": 1, "qtdLeitos": 30, "maxLeitosPorUh": 4}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EstabelecimentoService servico;

    @Test
    void listarComFiltros() throws Exception {
        when(servico.listar(VISUALIZADOR, "HOTEL", true)).thenReturn(List.of(
                new EstabelecimentoResumo(10L, "Hotel A", "HOTEL", Situacao.REGULAR, true, 10, 30)));

        mvc.perform(get("/api/estabelecimentos").param("tipo", "HOTEL").param("ativo", "true").with(visualizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeFantasia").value("Hotel A"))
                .andExpect(jsonPath("$[0].qtdLeitos").value(30));
    }

    @Test
    void buscarSemAcessoDa403() throws Exception {
        when(servico.buscar(10L, PARCEIRO)).thenThrow(new AccessDeniedException("não"));
        mvc.perform(get("/api/estabelecimentos/10").with(parceiro())).andExpect(status().isForbidden());
    }

    @Test
    void buscar() throws Exception {
        when(servico.buscar(10L, ADMIN)).thenReturn(HOTEL);
        mvc.perform(get("/api/estabelecimentos/10").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.nomeFantasia").value("Hotel A"))
                .andExpect(jsonPath("$.secretaria.tipoHospedagemCodigo").value("HOTEL"));
    }

    @Test
    void soAdminCria() throws Exception {
        String corpo = """
                {"dados": {"nomeFantasia": "Hotel A", "possuiCadastur": false},
                 "secretaria": {"tipoHospedagemCodigo": "HOTEL", "situacao": "REGULAR", "contatoNome": "Maria",
                                "aceitaPesquisas": true, "telefonePesquisas": "17999990000",
                                "emailPesquisas": "pesquisa@hotel.com", "destinoInteligente": false, "dadosPddt": false},
                 "capacidade": %s}
                """.formatted(CAPACIDADE_JSON);
        mvc.perform(post("/api/estabelecimentos").with(parceiro()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        when(servico.criar(new CriacaoRequest(Fixtures.dados("Hotel A"), Fixtures.secretaria("HOTEL"),
                new CapacidadeRequest(JAN, 10, 1, 30, 4)), ADMIN)).thenReturn(HOTEL);
        mvc.perform(post("/api/estabelecimentos").with(admin()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/estabelecimentos/10"));
    }

    @Test
    void criarComCnpjInvalidoDa400() throws Exception {
        mvc.perform(post("/api/estabelecimentos").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dados": {"nomeFantasia": "Hotel A", "cnpj": "12.345", "possuiCadastur": false},
                                 "secretaria": {"tipoHospedagemCodigo": "HOTEL", "situacao": "REGULAR", "aceitaPesquisas": false,
                                                "destinoInteligente": false, "dadosPddt": false},
                                 "capacidade": %s}
                                """.formatted(CAPACIDADE_JSON)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['dados.cnpj']").value("deve ter 14 dígitos, sem pontuação"));
        verify(servico, never()).criar(any(), any());
    }

    @Test
    void parceiroAtualizaEVisualizadorNao() throws Exception {
        String corpo = """
                {"dados": {"nomeFantasia": "Hotel A", "possuiCadastur": false}}
                """;
        when(servico.atualizar(10L, new AtualizacaoRequest(Fixtures.dados("Hotel A"), null), PARCEIRO)).thenReturn(HOTEL);

        mvc.perform(put("/api/estabelecimentos/10").with(parceiro()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
        mvc.perform(put("/api/estabelecimentos/10").with(visualizador()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isForbidden());
    }

    @Test
    void inativarEReativar() throws Exception {
        mvc.perform(post("/api/estabelecimentos/10/inativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(post("/api/estabelecimentos/10/reativar").with(admin())).andExpect(status().isNoContent());
        mvc.perform(post("/api/estabelecimentos/10/inativar").with(parceiro())).andExpect(status().isForbidden());
        verify(servico).definirAtivo(10L, false, ADMIN);
        verify(servico).definirAtivo(10L, true, ADMIN);
    }

    @Test
    void capacidades() throws Exception {
        when(servico.capacidades(10L, PARCEIRO)).thenReturn(List.of(CAPACIDADE));
        when(servico.registrarCapacidade(10L, new CapacidadeRequest(JAN, 10, 1, 30, 4), PARCEIRO)).thenReturn(CAPACIDADE);
        when(servico.corrigirCapacidade(10L, 5L, new CapacidadeRequest(JAN, 10, 1, 30, 4), ADMIN)).thenReturn(CAPACIDADE);

        mvc.perform(get("/api/estabelecimentos/10/capacidades").with(parceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leitosUteis").value(25.0));
        mvc.perform(post("/api/estabelecimentos/10/capacidades").with(parceiro())
                .contentType(MediaType.APPLICATION_JSON).content(CAPACIDADE_JSON)).andExpect(status().isCreated());
        mvc.perform(put("/api/estabelecimentos/10/capacidades/5").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content(CAPACIDADE_JSON)).andExpect(status().isOk());
        mvc.perform(put("/api/estabelecimentos/10/capacidades/5").with(parceiro())
                .contentType(MediaType.APPLICATION_JSON).content(CAPACIDADE_JSON)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/estabelecimentos/10/capacidades/5").with(admin())).andExpect(status().isNoContent());
        verify(servico).excluirCapacidade(10L, 5L, ADMIN);
    }

    @Test
    void capacidadeComMaisDe30LeitosPorUhDa400() throws Exception {
        mvc.perform(post("/api/estabelecimentos/10/capacidades").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vigenteDesde": "2026-01-01", "qtdUh": 10, "qtdUhPcd": 0, "qtdLeitos": 30,
                                 "maxLeitosPorUh": 31}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.maxLeitosPorUh").exists());
    }
}
