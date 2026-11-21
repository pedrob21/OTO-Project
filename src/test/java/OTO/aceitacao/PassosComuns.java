package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;
import tools.jackson.databind.JsonNode;

/** Massa de dados e passos genéricos: chamar uma rota e conferir status, erro e campos da resposta. */
public class PassosComuns {

    /** Respostas da massa (Plano de Testes, seção 3): quem envia, taxa (%) e diária (R$). */
    static final Map<String, String[]> RESPOSTAS_DA_MASSA = Map.of(
            "Hotel A", new String[] { "PARCEIRO A", "80,00", "500,00" },
            "Hotel B", new String[] { "PARCEIRO A", "50,00", "400,00" },
            "Pousada C", new String[] { "PARCEIRO C", "40,00", "300,00" });

    private final Api api;

    public PassosComuns(Api api) {
        this.api = api;
    }

    @Dado("o cenário de referência carregado")
    public void oCenarioDeReferenciaCarregado() {
        ResourceDatabasePopulator massa = new ResourceDatabasePopulator(
                new ClassPathResource("massa/cenario-de-referencia.sql"));
        massa.setSqlScriptEncoding("UTF-8");
        massa.execute(api.jdbc().getDataSource());
    }

    @Dado("as respostas da massa para o {string} e o {string}")
    public void asRespostasDaMassa(String primeiro, String segundo) {
        for (String estabelecimento : new String[] { primeiro, segundo }) {
            enviarRespostaDaMassa(RESPOSTAS_DA_MASSA.get(estabelecimento)[0], estabelecimento);
        }
    }

    @Dado("(que )o ADMIN lança/lançou a resposta da massa para o/a {string}")
    public void oAdminLancaARespostaDaMassa(String estabelecimento) {
        enviarRespostaDaMassa("ADMIN", estabelecimento);
    }

    @Dado("a campanha {string} publicada")
    public void aCampanhaPublicada(String campanha) {
        Response publicacao = api.chamar("ADMIN", "POST", "/api/campanhas/" + api.campanha(campanha) + "/publicacao");
        assertEquals(200, publicacao.statusCode(), publicacao.asString());
    }

    private void enviarRespostaDaMassa(String ator, String estabelecimento) {
        String[] massa = RESPOSTAS_DA_MASSA.get(estabelecimento);
        assertNotNull(massa, "sem resposta na massa para " + estabelecimento);
        Response resposta = api.chamar(ator, "PUT", rotaDaResposta(estabelecimento),
                Map.of("taxaOcupacao", Json.numero(massa[1]), "diariaMedia", Json.numero(massa[2])));
        assertEquals(200, resposta.statusCode(), resposta.asString());
    }

    String rotaDaResposta(String estabelecimento) {
        return "/api/campanhas/" + api.campanha(Api.CAMPANHA_DA_MASSA) + "/respostas/" + api.estabelecimento(estabelecimento);
    }

    // ------------------------------------------------------------------ genéricos

    @Quando("o {ator} chama {metodo} {string}")
    public void chama(String ator, String metodo, String rota) {
        api.chamar(ator, metodo, api.resolver(rota));
    }

    @Quando("o {ator} chama {metodo} {string} com:")
    public void chamaCom(String ator, String metodo, String rota, String corpo) {
        api.chamar(ator, metodo, api.resolver(rota), api.resolver(corpo));
    }

    @Entao("a resposta tem status {int}")
    public void aRespostaTemStatus(int status) {
        assertEquals(status, api.resposta().statusCode(), api.resposta().asString());
    }

    @Entao("a mensagem de erro é {string}")
    public void aMensagemDeErroE(String mensagem) {
        assertEquals(mensagem, api.corpo().path("detail").asString(), api.resposta().asString());
    }

    @Entao("o erro está no formato Problem Details com status {int}")
    public void oErroEstaNoFormatoProblemDetails(int status) {
        assertEquals(status, api.resposta().statusCode());
        String tipo = api.resposta().getHeader("Content-Type");
        assertTrue(tipo != null && tipo.startsWith("application/problem+json"), "Content-Type: " + tipo);
        JsonNode corpo = api.corpo();
        assertEquals(status, corpo.path("status").intValue());
        assertFalse(corpo.path("title").asString().isBlank(), "sem title");
    }

    @Entao("o erro aponta o campo {string}")
    public void oErroApontaOCampo(String campo) {
        assertTrue(api.corpo().path("campos").has(campo), api.resposta().asString());
    }

    @Entao("o campo {string} da resposta é {string}")
    public void oCampoDaRespostaE(String campo, String esperado) {
        Json.conferir(Json.caminho(api.corpo(), campo), esperado, campo);
    }

    static Map<String, Object> resposta(BigDecimal taxa, BigDecimal diaria) {
        return Map.of("taxaOcupacao", taxa, "diariaMedia", diaria);
    }
}
