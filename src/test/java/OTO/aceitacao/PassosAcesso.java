package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;
import tools.jackson.databind.JsonNode;

/** F01 e F03: login, convite de primeiro acesso e cadastro de usuários. */
public class PassosAcesso {

    private final Api api;

    public PassosAcesso(Api api) {
        this.api = api;
    }

    @Quando("{string} entra com a senha {string}")
    public void entraComASenha(String email, String senha) {
        api.guardar(api.entrar(email, senha));
    }

    @Entao("{string} consegue entrar com a senha {string}")
    public void consegueEntrar(String email, String senha) {
        Response login = api.entrar(email, senha);
        assertEquals(200, login.statusCode(), login.asString());
    }

    @Entao("{string} não consegue entrar com a senha {string}")
    public void naoConsegueEntrar(String email, String senha) {
        Response login = api.entrar(email, senha);
        assertEquals(401, login.statusCode(), login.asString());
    }

    @Entao("a resposta traz um token de acesso")
    public void aRespostaTrazUmToken() {
        JsonNode corpo = api.corpo();
        assertTrue(corpo.path("token").asString().length() > 20, "token ausente");
        assertFalse(corpo.path("expiraEm").asString().isBlank(), "expiraEm ausente");
    }

    @Dado("(que )o ADMIN cadastra/cadastrou o usuário {string} com o perfil {string}")
    public void oAdminCadastraOUsuario(String email, String perfil) {
        cadastrar(email, perfil, List.of());
    }

    @Quando("o ADMIN cadastra o parceiro {string} do estabelecimento {string}")
    public void oAdminCadastraOParceiro(String email, String estabelecimento) {
        cadastrar(email, "PARCEIRO", List.of(api.estabelecimento(estabelecimento)));
    }

    @Quando("o ADMIN cadastra o parceiro {string} sem estabelecimento")
    public void oAdminCadastraOParceiroSemEstabelecimento(String email) {
        cadastrar(email, "PARCEIRO", List.of());
    }

    private void cadastrar(String email, String perfil, List<Long> estabelecimentos) {
        Response resposta = api.chamar("ADMIN", "POST", "/api/usuarios", Map.of(
                "nome", "Usuário de teste", "email", email, "perfil", perfil, "estabelecimentoIds", estabelecimentos));
        if (resposta.statusCode() == 201) {
            api.guardarConvite(email, api.corpo().path("convite").path("token").asString());
        }
    }

    @Quando("o convite é usado para definir a senha {string}")
    public void oConviteEUsado(String senha) {
        api.chamar("ANÔNIMO", "POST", "/api/auth/definir-senha", Map.of("token", api.convite(), "novaSenha", senha));
    }

    @Dado("o convite venceu")
    public void oConviteVenceu() {
        int alterados = api.jdbc().update(
                "UPDATE token_usuario SET expira_em = now() - interval '1 minute' WHERE usuario_id = ?",
                api.usuario(api.emailDoConvite()));
        assertEquals(1, alterados, "convite não encontrado");
    }
}
