package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;
import tools.jackson.databind.JsonNode;

/** F08 a F11: prévia, publicação, cálculo conforme a planilha e divulgação com sigilo. */
public class PassosResultado {

    /** Nomes dos indicadores nas funcionalidades e os campos do resultado na API. */
    private static final Map<String, String> INDICADORES = Map.ofEntries(
            Map.entry("Taxa de ocupação (%)", "taxaOcupacao"),
            Map.entry("Fluxo em meios de hospedagem", "fluxoMh"),
            Map.entry("Fluxo da hospedagem caseira", "fluxoRhc"),
            Map.entry("Fluxo de day use", "fluxoDayUse"),
            Map.entry("Fluxo total de turistas", "fluxoTotal"),
            Map.entry("Diária média (R$)", "diariaMedia"),
            Map.entry("Receita da hospedagem (R$)", "receitaHospedagem"),
            Map.entry("Receita fora da hospedagem (R$)", "receitaSemHospedagem"),
            Map.entry("Leitos respondentes (%)", "percentualLeitosRespondentes"),
            Map.entry("Meios de hospedagem no cálculo", "qtdMh"),
            Map.entry("Leitos no cálculo (MH + RHC)", "leitosTotal"));
    private static final byte[] BOM_UTF8 = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    private final Api api;

    public PassosResultado(Api api) {
        this.api = api;
    }

    // ------------------------------------------------------------------ prévia e publicação

    @Quando("o ADMIN pede a prévia da campanha {string}")
    public void pedeAPrevia(String campanha) {
        api.chamar("ADMIN", "GET", "/api/campanhas/" + api.campanha(campanha) + "/previa");
    }

    @Entao("a prévia não permite publicar")
    public void aPreviaNaoPermitePublicar() {
        assertFalse(api.corpo().path("podePublicar").booleanValue());
    }

    @Entao("a prévia aponta o impedimento {string}")
    public void aPreviaApontaOImpedimento(String impedimento) {
        assertTrue(textos(api.corpo().path("impedimentos")).contains(impedimento), api.resposta().asString());
    }

    @Entao("a prévia aponta o impedimento de {percentual} dos leitos")
    public void aPreviaApontaOImpedimentoDeLeitos(BigDecimal percentual) {
        JsonNode previa = api.corpo();
        Json.igual(previa.get("percentualLeitosRespondentes"), percentual, "percentualLeitosRespondentes");
        String inicio = "Leitos respondentes: " + percentual.toPlainString().replace('.', ',') + "%";
        assertTrue(textos(previa.path("impedimentos")).stream().anyMatch(i -> i.startsWith(inicio)),
                previa.path("impedimentos").toString());
    }

    @Entao("a prévia não aponta impedimento de leitos")
    public void aPreviaNaoApontaImpedimentoDeLeitos() {
        assertTrue(textos(api.corpo().path("impedimentos")).stream().noneMatch(i -> i.startsWith("Leitos respondentes")),
                api.corpo().path("impedimentos").toString());
    }

    @Entao("a prévia avisa {string}")
    public void aPreviaAvisa(String aviso) {
        assertTrue(textos(api.corpo().path("avisos")).contains(aviso), api.corpo().path("avisos").toString());
    }

    @Quando("o ADMIN publica a campanha {string}")
    public void publicaACampanha(String campanha) {
        api.chamar("ADMIN", "POST", "/api/campanhas/" + api.campanha(campanha) + "/publicacao");
    }

    @Quando("o ADMIN cancela a publicação da campanha {string}")
    public void cancelaAPublicacao(String campanha) {
        api.chamar("ADMIN", "DELETE", "/api/campanhas/" + api.campanha(campanha) + "/publicacao");
    }

    @Entao("o {ator} não encontra o resultado da campanha {string}")
    public void naoEncontraOResultado(String ator, String campanha) {
        Response consulta = api.consultar(ator, "/api/campanhas/" + api.campanha(campanha) + "/resultado");
        assertEquals(404, consulta.statusCode(), consulta.asString());
    }

    @Entao("o {ator} vê a campanha {string} entre as publicadas")
    public void veACampanhaEntreAsPublicadas(String ator, String campanha) {
        Response consulta = api.consultar(ator, "/api/campanhas/publicadas");
        List<String> nomes = new ArrayList<>();
        for (JsonNode publicada : api.corpoDe(consulta)) {
            nomes.add(publicada.path("nome").asString());
        }
        assertTrue(nomes.contains(campanha), nomes.toString());
    }

    // ------------------------------------------------------------------ cálculo

    @Entao("os indicadores calculados são:")
    public void osIndicadoresCalculadosSao(DataTable tabela) {
        JsonNode resultado = api.resultado();
        for (Map<String, String> linha : tabela.asMaps()) {
            String campo = INDICADORES.get(linha.get("indicador"));
            assertNotNull(campo, "indicador desconhecido: " + linha.get("indicador"));
            Json.igual(resultado.get(campo), Json.numero(linha.get("valor")), linha.get("indicador"));
        }
    }

    @Entao("a faixa de ocupação é {string}")
    public void aFaixaDeOcupacaoE(String faixa) {
        assertEquals(faixa, api.resultado().path("faixa").path("nome").asString());
    }

    @Entao("o tipo {string} na prévia tem diária de R$ {valor}, RevPAR de R$ {valor} e receita de R$ {valor}")
    public void oTipoNaPreviaTem(String tipo, BigDecimal diaria, BigDecimal revpar, BigDecimal receita) {
        JsonNode linha = tipo(api.resultado(), tipo);
        Json.igual(linha.get("diariaMedia"), diaria, "diariaMedia");
        Json.igual(linha.get("revpar"), revpar, "revpar");
        Json.igual(linha.get("receitaEstimada"), receita, "receitaEstimada");
    }

    /** Nova vigência de parâmetros a partir de 01/04/2026, igual à da V7 a não ser pelo day use. */
    @Dado("o day use de {percentual} vigente no período")
    public void oDayUseVigenteNoPeriodo(BigDecimal dayUse) {
        Response criado = api.chamar("ADMIN", "POST", "/api/parametros-calculo",
                PassosHistorico.parametros("2026-04-01", dayUse, new BigDecimal("70.00")));
        assertEquals(201, criado.statusCode(), criado.asString());
    }

    @Entao("o fluxo total de turistas é {valor}")
    public void oFluxoTotalE(BigDecimal fluxo) {
        Json.igual(api.resultado().get("fluxoTotal"), fluxo, "fluxoTotal");
    }

    // ------------------------------------------------------------------ divulgação com sigilo

    @Quando("o {ator} consulta o resultado da campanha {string}")
    public void consultaOResultado(String ator, String campanha) {
        api.chamar(ator, "GET", "/api/campanhas/" + api.campanha(campanha) + "/resultado");
    }

    @Entao("o tipo {string} aparece com os valores ocultos")
    public void oTipoApareceOculto(String tipo) {
        JsonNode linha = tipo(api.corpo(), tipo);
        assertTrue(linha.path("omitido").booleanValue(), linha.toString());
        assertTrue(linha.path("taxaOcupacao").isNull(), linha.toString());
        assertTrue(linha.path("receitaEstimada").isNull(), linha.toString());
    }

    @Entao("o tipo {string} aparece com a taxa de ocupação de {percentual}")
    public void oTipoApareceComATaxa(String tipo, BigDecimal taxa) {
        JsonNode linha = tipo(api.corpo(), tipo);
        assertFalse(linha.path("omitido").booleanValue(), linha.toString());
        Json.igual(linha.get("taxaOcupacao"), taxa, "taxaOcupacao do tipo " + tipo);
    }

    @Quando("o {ator} baixa o CSV do resultado da campanha {string}")
    public void baixaOCsv(String ator, String campanha) {
        api.chamar(ator, "GET", "/api/campanhas/" + api.campanha(campanha) + "/resultado.csv");
    }

    @Entao("o CSV está no formato do Excel em português")
    public void oCsvEstaNoFormatoDoExcel() {
        String tipo = api.resposta().getHeader("Content-Type");
        assertTrue(tipo != null && tipo.startsWith("text/csv"), "Content-Type: " + tipo);
        byte[] bytes = api.resposta().asByteArray();
        assertTrue(bytes.length > 3 && Arrays.equals(BOM_UTF8, Arrays.copyOf(bytes, 3)), "o CSV não começa com o BOM do UTF-8");
    }

    @Entao("o CSV contém a linha {string}")
    public void oCsvContemALinha(String linha) {
        assertTrue(linhasDoCsv().contains(linha), String.join("\n", linhasDoCsv()));
    }

    @Entao("o CSV não contém {string}")
    public void oCsvNaoContem(String texto) {
        assertFalse(textoDoCsv().contains(texto), textoDoCsv());
    }

    private String textoDoCsv() {
        String texto = new String(api.resposta().asByteArray(), StandardCharsets.UTF_8);
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }

    private List<String> linhasDoCsv() {
        return textoDoCsv().lines().toList();
    }

    private static JsonNode tipo(JsonNode resultado, String codigo) {
        for (JsonNode linha : resultado.path("porTipo")) {
            if (codigo.equals(linha.path("tipoHospedagemCodigo").asString())) {
                return linha;
            }
        }
        return fail("tipo " + codigo + " ausente: " + resultado.path("porTipo"));
    }

    private static List<String> textos(JsonNode lista) {
        List<String> textos = new ArrayList<>();
        for (JsonNode item : lista) {
            textos.add(item.asString());
        }
        return textos;
    }
}
