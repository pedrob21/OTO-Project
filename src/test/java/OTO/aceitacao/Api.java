package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import tools.jackson.databind.JsonNode;

/**
 * Cliente da API para os passos: faz login com os usuários da massa, chama as rotas e guarda a
 * última resposta. Também acha no banco os ids dos registros da massa pelo nome
 * ("Hotel A", "1º de Maio 2026", "001/26", "admin@oto.test").
 */
public class Api {

    public static final String SENHA_DA_MASSA = "Senha@Teste1";
    public static final String CAMPANHA_DA_MASSA = "1º de Maio 2026";

    private static final Map<String, String> ATORES = Map.of(
            "ADMIN", "admin@oto.test",
            "PARCEIRO A", "parceiro.a@oto.test",
            "PARCEIRO C", "parceiro.c@oto.test",
            "VISUALIZADOR", "visualizador@oto.test");
    private static final String ANONIMO = "ANÔNIMO";
    private static final Pattern MARCADOR = Pattern.compile("\\{([^{}\"]+)\\}");
    private static final Pattern CODIGO_DE_PESQUISA = Pattern.compile("\\d{3}/\\d{2}");

    private final JdbcTemplate jdbc;
    private final Environment ambiente;
    private final Map<String, String> tokens = new HashMap<>();

    private Response resposta;
    private String convite;
    private String emailDoConvite;

    public Api(JdbcTemplate jdbc, Environment ambiente) {
        this.jdbc = jdbc;
        this.ambiente = ambiente;
    }

    // ------------------------------------------------------------------ chamadas

    /** Chama a rota como o ator ("ADMIN", "PARCEIRO A"...) ou como o e-mail informado; corpo pode ser nulo. */
    public Response chamar(String ator, String metodo, String rota, Object corpo) {
        RequestSpecification requisicao = requisicao(ator);
        if (corpo != null) {
            String json = corpo instanceof String texto ? texto : Json.escrever(corpo);
            requisicao.contentType("application/json; charset=UTF-8").body(json.getBytes(StandardCharsets.UTF_8));
        }
        return guardar(requisicao.request(metodo, rota));
    }

    public Response chamar(String ator, String metodo, String rota) {
        return chamar(ator, metodo, rota, null);
    }

    /** Envia um arquivo CSV como o ADMIN (importação da série histórica). */
    public Response enviarArquivo(String rota, String conteudo, Charset codificacao, String fonte) {
        RequestSpecification requisicao = requisicao("ADMIN")
                .multiPart("arquivo", "historico.csv", conteudo.getBytes(codificacao), "text/csv")
                .multiPart("fonte", fonte);
        return guardar(requisicao.post(rota));
    }

    /** Login direto, sem guardar o token (o próprio login é o que está em teste). */
    public Response entrar(String email, String senha) {
        return RestAssured.given().baseUri("http://localhost").port(porta())
                .contentType("application/json; charset=UTF-8")
                .body(Json.escrever(Map.of("email", email, "senha", senha)).getBytes(StandardCharsets.UTF_8))
                .post("/api/auth/login");
    }

    /** GET para conferir o estado, sem trocar a última resposta do cenário. */
    public Response consultar(String ator, String rota) {
        return requisicao(ator).get(rota);
    }

    /** GET como ADMIN que precisa dar certo, sem trocar a última resposta do cenário. */
    public Response consultarComoAdmin(String rota) {
        Response consulta = consultar("ADMIN", rota);
        assertEquals(200, consulta.statusCode(), rota + ": " + consulta.asString());
        return consulta;
    }

    public JsonNode corpoDe(Response qualquer) {
        return Json.ler(qualquer.asString());
    }

    public Response guardar(Response nova) {
        this.resposta = nova;
        return nova;
    }

    public Response resposta() {
        return resposta;
    }

    public JsonNode corpo() {
        return Json.ler(resposta.asString());
    }

    /** O resultado da campanha: na prévia ele vem dentro de "resultado". */
    public JsonNode resultado() {
        JsonNode corpo = corpo();
        return corpo.has("resultado") ? corpo.get("resultado") : corpo;
    }

    private RequestSpecification requisicao(String ator) {
        RequestSpecification requisicao = RestAssured.given().baseUri("http://localhost").port(porta());
        String email = ATORES.getOrDefault(ator, ator);
        if (!ANONIMO.equals(ator)) {
            requisicao.header("Authorization", "Bearer " + token(email));
        }
        return requisicao;
    }

    private String token(String email) {
        return tokens.computeIfAbsent(email, e -> {
            Response login = entrar(e, SENHA_DA_MASSA);
            assertEquals(200, login.statusCode(), "login de " + e + ": " + login.asString());
            return Json.ler(login.asString()).get("token").stringValue();
        });
    }

    private int porta() {
        return Integer.parseInt(ambiente.getRequiredProperty("local.server.port"));
    }

    // ------------------------------------------------------------------ convite

    public void guardarConvite(String email, String token) {
        this.emailDoConvite = email.toLowerCase();
        this.convite = token;
    }

    public String convite() {
        return convite;
    }

    public String emailDoConvite() {
        return emailDoConvite;
    }

    // ------------------------------------------------------------------ ids da massa

    public long estabelecimento(String nome) {
        return id("SELECT id FROM estabelecimento WHERE nome_fantasia = ?", nome);
    }

    public long campanha(String nome) {
        return id("SELECT id FROM campanha WHERE nome = ?", nome);
    }

    public long rodada(String codigo) {
        return id("SELECT id FROM rodada_pesquisa WHERE codigo = ?", codigo);
    }

    public long usuario(String email) {
        return id("SELECT id FROM usuario WHERE email = ?", email);
    }

    public long grupoDoInventario(String codigo) {
        return id("SELECT id FROM inventario_grupo WHERE codigo = ?", codigo);
    }

    /** Item do catálogo pelo código "GRUPO.ITEM", ex.: GERAL.ESTACIONAMENTO. */
    public long itemDoInventario(String codigo) {
        String[] partes = codigo.split("\\.", 2);
        return id("SELECT i.id FROM inventario_item i JOIN inventario_grupo g ON g.id = i.grupo_id "
                + "WHERE g.codigo = ? AND i.codigo = ?", partes[0], partes[1]);
    }

    public long respostaDo(String estabelecimento) {
        return id("SELECT r.id FROM resposta_ocupacao r JOIN estabelecimento e ON e.id = r.estabelecimento_id "
                + "WHERE e.nome_fantasia = ?", estabelecimento);
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    /** Troca {Hotel A}, {1º de Maio 2026}, {001/26} e {email} pelos ids. Objetos JSON ficam como estão. */
    public String resolver(String texto) {
        Matcher m = MARCADOR.matcher(texto);
        StringBuilder saida = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(saida, Long.toString(idDoMarcador(m.group(1))));
        }
        m.appendTail(saida);
        return saida.toString();
    }

    private long idDoMarcador(String chave) {
        if (chave.contains("@")) {
            return usuario(chave);
        }
        if (CODIGO_DE_PESQUISA.matcher(chave).matches()) {
            return rodada(chave);
        }
        List<Long> estabelecimentos = jdbc.queryForList("SELECT id FROM estabelecimento WHERE nome_fantasia = ?",
                Long.class, chave);
        return estabelecimentos.isEmpty() ? campanha(chave) : estabelecimentos.get(0);
    }

    private long id(String sql, Object... parametros) {
        List<Long> ids = jdbc.queryForList(sql, Long.class, parametros);
        assertEquals(1, ids.size(), "registro da massa não encontrado: " + String.join(", ", List.of(parametros).stream()
                .map(String::valueOf).toList()));
        return ids.get(0);
    }
}
