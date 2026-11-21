package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import tools.jackson.databind.JsonNode;

/** F04 e F05: estabelecimentos, capacidade e itens do inventário. */
public class PassosInventario {

    private final Api api;

    public PassosInventario(Api api) {
        this.api = api;
    }

    @Quando("o {ator} altera o {string} com o telefone comercial {string} e a situação {string}")
    public void altera(String ator, String estabelecimento, String telefone, String situacao) {
        long id = api.estabelecimento(estabelecimento);
        JsonNode atual = api.corpoDe(api.consultarComoAdmin("/api/estabelecimentos/" + id));
        Map<String, Object> dados = Json.comoMapa(atual.get("dados"));
        Map<String, Object> secretaria = Json.comoMapa(atual.get("secretaria"));
        dados.put("telefoneComercial", telefone);
        secretaria.put("situacao", situacao);
        api.chamar(ator, "PUT", "/api/estabelecimentos/" + id, Map.of("dados", dados, "secretaria", secretaria));
    }

    @Entao("o {string} tem o telefone comercial {string} e a situação {string}")
    public void temOTelefoneEASituacao(String estabelecimento, String telefone, String situacao) {
        JsonNode atual = api.corpoDe(api.consultarComoAdmin("/api/estabelecimentos/" + api.estabelecimento(estabelecimento)));
        assertEquals(telefone, atual.path("dados").path("telefoneComercial").asString());
        assertEquals(situacao, atual.path("secretaria").path("situacao").asString());
    }

    @Dado("(que )o ADMIN cadastra/cadastrou o hotel {string} com o CNPJ {string}, {int} UH, {int} leitos e no máximo {int} pessoas por UH")
    public void oAdminCadastraOHotel(String nome, String cnpj, int uh, int leitos, int maximo) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nomeFantasia", nome);
        dados.put("cnpj", cnpj);
        dados.put("possuiCadastur", false);
        api.chamar("ADMIN", "POST", "/api/estabelecimentos", Map.of(
                "dados", dados,
                "secretaria", Map.of("tipoHospedagemCodigo", "HOTEL", "situacao", "REGULAR", "aceitaPesquisas", true),
                "capacidade", capacidade(LocalDate.of(2026, 1, 1), uh, leitos, maximo)));
    }

    @Dado("(que )o {ator} registra/registrou para o {string} a capacidade de {int} UH e {int} leitos, no máximo {int} por UH, a partir de {data}")
    public void registraACapacidade(String ator, String estabelecimento, int uh, int leitos, int maximo, LocalDate data) {
        api.chamar(ator, "POST", "/api/estabelecimentos/" + api.estabelecimento(estabelecimento) + "/capacidades",
                capacidade(data, uh, leitos, maximo));
    }

    @Entao("o {string} tem {int} vigências de capacidade")
    public void temVigenciasDeCapacidade(String estabelecimento, int quantidade) {
        JsonNode lista = api.corpoDe(api.consultarComoAdmin(
                "/api/estabelecimentos/" + api.estabelecimento(estabelecimento) + "/capacidades"));
        assertEquals(quantidade, lista.size(), lista.toString());
    }

    private static Map<String, Object> capacidade(LocalDate desde, int uh, int leitos, int maximo) {
        return Map.of("vigenteDesde", desde.toString(), "qtdUh", uh, "qtdUhPcd", 0, "qtdLeitos", leitos,
                "maxLeitosPorUh", maximo);
    }

    // ------------------------------------------------------------------ inventário

    @Dado("(que )o {ator} marca/marcou no inventário do {string} os itens:")
    public void marcaOsItens(String ator, String estabelecimento, DataTable itens) {
        api.chamar(ator, "PUT", "/api/estabelecimentos/" + api.estabelecimento(estabelecimento) + "/inventario",
                Map.of("itens", itensDaTabela(itens)));
    }

    @Entao("o inventário do {string} tem os itens:")
    public void oInventarioTemOsItens(String estabelecimento, DataTable itens) {
        JsonNode atual = api.corpoDe(api.consultarComoAdmin(
                "/api/estabelecimentos/" + api.estabelecimento(estabelecimento) + "/inventario"));
        Set<String> marcados = new TreeSet<>();
        for (JsonNode item : atual.path("itens")) {
            marcados.add(item.path("itemId").asString() + "|" + textoOuVazio(item.get("complemento")));
        }
        Set<String> esperados = new TreeSet<>();
        for (Map<String, Object> item : itensDaTabela(itens)) {
            Object complemento = item.get("complemento");
            esperados.add(item.get("itemId") + "|" + (complemento == null ? "" : complemento));
        }
        assertEquals(esperados, marcados);
    }

    @Quando("o ADMIN tenta excluir do catálogo o item {string}")
    public void oAdminTentaExcluirOItem(String item) {
        api.chamar("ADMIN", "DELETE", "/api/inventario/itens/" + api.itemDoInventario(item));
    }

    @Quando("o ADMIN inativa no catálogo o item {string}")
    public void oAdminInativaOItem(String item) {
        api.chamar("ADMIN", "POST", "/api/inventario/itens/" + api.itemDoInventario(item) + "/inativar");
    }

    @Entao("o formulário do inventário não oferece o item {string}")
    public void oFormularioNaoOfereceOItem(String item) {
        long id = api.itemDoInventario(item);
        JsonNode catalogo = api.corpoDe(api.consultarComoAdmin("/api/inventario/catalogo"));
        for (JsonNode grupo : catalogo) {
            for (JsonNode oferecido : grupo.path("itens")) {
                assertFalse(oferecido.path("id").longValue() == id, "o item " + item + " ainda aparece no formulário");
            }
        }
    }

    @Quando("o ADMIN tenta excluir do catálogo o grupo {string}")
    public void oAdminTentaExcluirOGrupo(String grupo) {
        api.chamar("ADMIN", "DELETE", "/api/inventario/grupos/" + api.grupoDoInventario(grupo));
    }

    private List<Map<String, Object>> itensDaTabela(DataTable tabela) {
        List<Map<String, Object>> itens = new ArrayList<>();
        for (Map<String, String> linha : tabela.asMaps()) {
            Map<String, Object> item = new HashMap<>();
            item.put("itemId", api.itemDoInventario(linha.get("item")));
            String complemento = linha.get("complemento");
            item.put("complemento", complemento == null || complemento.isBlank() ? null : complemento);
            itens.add(item);
        }
        return itens;
    }

    private static String textoOuVazio(JsonNode valor) {
        return valor == null || valor.isNull() ? "" : valor.asString();
    }
}
