package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;
import tools.jackson.databind.JsonNode;

/** F12 e F13: série histórica, painéis, parâmetros de cálculo e referências. */
public class PassosHistorico {

    private static final String FONTE = "Indicadores do Turismo 2017-2025";

    private final Api api;

    public PassosHistorico(Api api) {
        this.api = api;
    }

    // ------------------------------------------------------------------ série histórica

    @Quando("o ADMIN importa a série histórica do arquivo:")
    public void importa(String conteudo) {
        api.enviarArquivo("/api/historico/importacao", conteudo + "\n", StandardCharsets.UTF_8, FONTE);
    }

    /** O Excel em português no Windows salva o CSV em Windows-1252. */
    @Quando("o ADMIN importa a série histórica do arquivo salvo pelo Excel do Windows:")
    public void importaDoExcelDoWindows(String conteudo) {
        api.enviarArquivo("/api/historico/importacao", conteudo + "\n", Charset.forName("windows-1252"), FONTE);
    }

    @Entao("a importação informa {int} linhas lidas, {int} inseridas e {int} atualizadas")
    public void aImportacaoInforma(int lidas, int inseridas, int atualizadas) {
        JsonNode corpo = api.corpo();
        assertEquals(lidas, corpo.path("linhasLidas").intValue(), corpo.toString());
        assertEquals(inseridas, corpo.path("inseridos").intValue(), corpo.toString());
        assertEquals(atualizadas, corpo.path("atualizados").intValue(), corpo.toString());
    }

    @Entao("a importação aponta o erro {string} na linha {int}")
    public void aImportacaoApontaOErro(String mensagem, int linha) {
        for (JsonNode erro : api.corpo().path("erros")) {
            if (erro.path("linha").intValue() == linha && mensagem.equals(erro.path("mensagem").asString())) {
                return;
            }
        }
        fail("erro não apontado: " + api.corpo().path("erros"));
    }

    @Entao("a série histórica tem {int} valores")
    public void aSerieHistoricaTem(int quantidade) {
        assertEquals(quantidade, serieHistorica().size());
    }

    @Entao("a série histórica guarda o fluxo de turistas de {valor} no feriado {string} de {int}")
    public void guardaOFluxoNoFeriado(BigDecimal fluxo, String feriado, int ano) {
        for (JsonNode valor : serieHistorica()) {
            if ("FLUXO_TURISTAS".equals(valor.path("indicador").asString())
                    && feriado.equals(valor.path("feriadoCodigo").asString()) && valor.path("ano").intValue() == ano) {
                Json.igual(valor.get("valor"), fluxo, "valor");
                return;
            }
        }
        fail("fluxo do feriado " + feriado + " de " + ano + " ausente");
    }

    @Entao("a série histórica guarda a observação {string}")
    public void guardaAObservacao(String observacao) {
        List<String> observacoes = new ArrayList<>();
        for (JsonNode valor : serieHistorica()) {
            observacoes.add(valor.path("observacao").asString());
        }
        assertTrue(observacoes.contains(observacao), observacoes.toString());
    }

    @Dado("(que )o ADMIN grava/gravou na série histórica a taxa de ocupação de {percentual} do feriado {string} de {int}")
    public void gravaATaxaDoFeriado(BigDecimal taxa, String feriado, int ano) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("indicador", "TAXA_OCUPACAO");
        corpo.put("modalidade", "CONSOLIDADA");
        corpo.put("ano", ano);
        corpo.put("mes", null);
        corpo.put("feriadoCodigo", feriado);
        corpo.put("tipoHospedagemCodigo", null);
        corpo.put("valor", taxa);
        corpo.put("observacao", null);
        corpo.put("fonte", FONTE);
        Response gravado = api.chamar("ADMIN", "POST", "/api/historico", corpo);
        assertTrue(gravado.statusCode() == 200 || gravado.statusCode() == 201, gravado.asString());
    }

    private JsonNode serieHistorica() {
        return api.corpoDe(api.consultarComoAdmin("/api/historico"));
    }

    // ------------------------------------------------------------------ painéis

    @Quando("o {ator} consulta o painel do feriado {string} de {int}")
    public void consultaOPainelDoFeriado(String ator, String feriado, int ano) {
        api.chamar(ator, "GET", "/api/dashboard/feriados?feriado=" + feriado + "&anoInicio=" + ano + "&anoFim=" + ano);
    }

    @Entao("o painel mostra a taxa de ocupação de {percentual} com origem {string}")
    public void oPainelMostra(BigDecimal taxa, String origem) {
        JsonNode ano = api.corpo().path(0).path("anos").path(0);
        Json.igual(ano.get("taxaOcupacao"), taxa, "taxaOcupacao");
        assertEquals(origem, ano.path("origem").asString());
    }

    @Quando("o {ator} consulta o resumo do painel")
    public void consultaOResumoDoPainel(String ator) {
        api.chamar(ator, "GET", "/api/dashboard/resumo");
    }

    // ------------------------------------------------------------------ parâmetros de cálculo

    @Dado("(que )o ADMIN cadastra/cadastrou os parâmetros de cálculo a partir de {data} com mínimo de {percentual} dos leitos")
    public void cadastraOsParametros(LocalDate desde, BigDecimal minimo) {
        api.chamar("ADMIN", "POST", "/api/parametros-calculo", parametros(desde.toString(), new BigDecimal("15.15"), minimo));
    }

    @Quando("o ADMIN tenta alterar os parâmetros usados na campanha {string}")
    public void tentaAlterarOsParametros(String campanha) {
        long id = parametroDaCampanha(campanha);
        JsonNode atual = null;
        for (JsonNode vigencia : api.corpoDe(api.consultarComoAdmin("/api/parametros-calculo"))) {
            if (vigencia.path("id").longValue() == id) {
                atual = vigencia;
            }
        }
        Map<String, Object> corpo = Json.comoMapa(atual);
        corpo.remove("id");
        corpo.remove("registradoEm");
        corpo.put("fonte", "Tentativa de correção");
        api.chamar("ADMIN", "PUT", "/api/parametros-calculo/" + id, corpo);
    }

    @Quando("o ADMIN tenta excluir os parâmetros usados na campanha {string}")
    public void tentaExcluirOsParametros(String campanha) {
        api.chamar("ADMIN", "DELETE", "/api/parametros-calculo/" + parametroDaCampanha(campanha));
    }

    private long parametroDaCampanha(String campanha) {
        return api.corpoDe(api.consultarComoAdmin("/api/campanhas/" + api.campanha(campanha) + "/resultado"))
                .path("parametroCalculoId").longValue();
    }

    /** Os mesmos parâmetros da migração V7, com day use e mínimo de leitos à escolha do cenário. */
    static Map<String, Object> parametros(String desde, BigDecimal dayUse, BigDecimal minimoLeitos) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("vigenteDesde", desde);
        corpo.put("percentualDayUse", dayUse);
        corpo.put("diasPermanenciaMedia", 4);
        corpo.put("diasEmParques", 2);
        corpo.put("gastoMedioDiario", new BigDecimal("250.00"));
        corpo.put("gastoMedioIngresso", new BigDecimal("105.00"));
        corpo.put("percentualMinimoLeitosPublicacao", minimoLeitos);
        corpo.put("fonte", "Cenário de teste");
        return corpo;
    }
}
