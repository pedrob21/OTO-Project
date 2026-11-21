package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;
import tools.jackson.databind.JsonNode;

/** F06, F07 e F08: ciclo da pesquisa, respostas dos parceiros e diárias digitadas pela equipe. */
public class PassosPesquisa {

    private final Api api;

    public PassosPesquisa(Api api) {
        this.api = api;
    }

    // ------------------------------------------------------------------ pesquisa (rodada)

    @Dado("(que )o ADMIN cria/criou a pesquisa {string} com a campanha mensal {string} de {data} a {data}")
    public void criaAPesquisaComCampanha(String codigo, String campanha, LocalDate inicio, LocalDate fim) {
        criarPesquisa(codigo);
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", campanha);
        corpo.put("modalidade", "CONSOLIDADA");
        corpo.put("tipoPeriodo", "MENSAL");
        corpo.put("feriadoCodigo", null);
        corpo.put("periodoInicio", inicio.toString());
        corpo.put("periodoFim", fim.toString());
        corpo.put("qtdDiarias", ChronoUnit.DAYS.between(inicio, fim) + 1);
        Response criada = api.chamar("ADMIN", "POST", "/api/rodadas/" + api.rodada(codigo) + "/campanhas", corpo);
        assertEquals(201, criada.statusCode(), criada.asString());
    }

    @Dado("(que )o ADMIN cria/criou a pesquisa {string} sem campanhas")
    public void criaAPesquisaSemCampanhas(String codigo) {
        criarPesquisa(codigo);
    }

    private void criarPesquisa(String codigo) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("codigo", codigo);
        corpo.put("titulo", "Pesquisa " + codigo);
        corpo.put("dataAbertura", null);
        corpo.put("prazoResposta", null);
        Response criada = api.chamar("ADMIN", "POST", "/api/rodadas", corpo);
        assertEquals(201, criada.statusCode(), criada.asString());
    }

    /** O prazo é contado a partir de hoje, porque a abertura sem data vale para o dia em que o teste roda. */
    @Dado("(que )o ADMIN abre/abriu a pesquisa {string} com prazo de {int} dias")
    public void abreAPesquisa(String codigo, int dias) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("dataAbertura", null);
        corpo.put("prazoResposta", LocalDate.now().plusDays(dias).toString());
        api.chamar("ADMIN", "POST", "/api/rodadas/" + api.rodada(codigo) + "/abrir", corpo);
    }

    @Dado("(que )o ADMIN encerra/encerrou a pesquisa {string}")
    public void encerraAPesquisa(String codigo) {
        api.chamar("ADMIN", "POST", "/api/rodadas/" + api.rodada(codigo) + "/encerrar");
    }

    @Quando("o ADMIN reabre a pesquisa {string} com prazo de {int} dias")
    public void reabreAPesquisa(String codigo, int dias) {
        api.chamar("ADMIN", "POST", "/api/rodadas/" + api.rodada(codigo) + "/reabrir",
                Map.of("prazoResposta", LocalDate.now().plusDays(dias).toString()));
    }

    @Entao("a pesquisa {string} está com o status {string}")
    public void aPesquisaEstaComOStatus(String codigo, String status) {
        JsonNode pesquisa = api.corpoDe(api.consultarComoAdmin("/api/rodadas/" + api.rodada(codigo)));
        assertEquals(status, pesquisa.path("status").asString());
    }

    // ------------------------------------------------------------------ respostas

    @Dado("(que )o {ator} envia/enviou a resposta do/da {string}: taxa {string}% e diária R$ {string}")
    public void enviaAResposta(String ator, String estabelecimento, String taxa, String diaria) {
        api.chamar(ator, "PUT", "/api/campanhas/" + api.campanha(Api.CAMPANHA_DA_MASSA) + "/respostas/"
                + api.estabelecimento(estabelecimento), PassosComuns.resposta(Json.numero(taxa), Json.numero(diaria)));
    }

    @Entao("a resposta do/da {string} está na versão {int} com taxa de {percentual}")
    public void aRespostaEstaNaVersao(String estabelecimento, int versao, BigDecimal taxa) {
        JsonNode resposta = respostaDaCampanha(estabelecimento);
        assertEquals(versao, resposta.path("versao").intValue());
        Json.igual(resposta.get("taxaOcupacao"), taxa, "taxaOcupacao");
    }

    @Entao("o histórico da resposta do/da {string} tem {int} versões")
    public void oHistoricoTemVersoes(String estabelecimento, int versoes) {
        JsonNode historico = api.corpoDe(api.consultarComoAdmin("/api/respostas/" + api.respostaDo(estabelecimento) + "/historico"));
        assertEquals(versoes, historico.size(), historico.toString());
    }

    @Entao("a resposta lista os estabelecimentos {string}")
    public void aRespostaListaOsEstabelecimentos(String nomes) {
        List<String> esperados = new ArrayList<>(Arrays.stream(nomes.split(",")).map(String::trim).toList());
        List<String> listados = new ArrayList<>();
        for (JsonNode item : api.corpo()) {
            listados.add(item.path("nomeFantasia").asString());
        }
        esperados.sort(null);
        listados.sort(null);
        assertEquals(esperados, listados);
    }

    @Entao("a resposta do/da {string} foi enviada pelo usuário {string}")
    public void aRespostaFoiEnviadaPor(String estabelecimento, String email) {
        assertEquals(api.usuario(email), respostaDaCampanha(estabelecimento).path("enviadoPor").longValue());
    }

    @Quando("o {ator} consulta o histórico da resposta do/da {string}")
    public void consultaOHistoricoDaResposta(String ator, String estabelecimento) {
        api.chamar(ator, "GET", "/api/respostas/" + api.respostaDo(estabelecimento) + "/historico");
    }

    private JsonNode respostaDaCampanha(String estabelecimento) {
        JsonNode respostas = api.corpoDe(api.consultarComoAdmin(
                "/api/campanhas/" + api.campanha(Api.CAMPANHA_DA_MASSA) + "/respostas"));
        for (JsonNode resposta : respostas) {
            if (estabelecimento.equals(resposta.path("nomeFantasia").asString())) {
                return resposta;
            }
        }
        return fail("sem resposta do " + estabelecimento + ": " + respostas);
    }

    // ------------------------------------------------------------------ diárias digitadas

    /** Tipo "geral" = diária média geral (tipoHospedagemCodigo nulo). */
    @Dado("(que )o ADMIN digita/digitou para a campanha {string} a diária média de R$ {valor} do tipo {string}")
    public void digitaADiaria(String campanha, BigDecimal diaria, String tipo) {
        Map<String, Object> linha = new HashMap<>();
        linha.put("tipoHospedagemCodigo", "geral".equals(tipo) ? null : tipo);
        linha.put("diariaMedia", diaria);
        api.chamar("ADMIN", "PUT", "/api/campanhas/" + api.campanha(campanha) + "/diarias",
                Map.of("diarias", List.of(linha), "fonte", "Pesquisa de tarifas"));
    }

    @Quando("o ADMIN apaga as diárias digitadas da campanha {string}")
    public void apagaAsDiarias(String campanha) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("diarias", List.of());
        corpo.put("fonte", null);
        api.chamar("ADMIN", "PUT", "/api/campanhas/" + api.campanha(campanha) + "/diarias", corpo);
    }
}
