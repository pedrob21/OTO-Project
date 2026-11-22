package OTO.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.usuario.Usuario;

class TokenUsuarioRepositoryTest extends BaseRepositorioTest {

    private static final Instant AMANHA = Instant.now().plusSeconds(86_400);

    @Autowired
    private TokenUsuarioRepository repository;

    private Usuario ana;
    private Usuario bruno;

    private void usuarios() {
        ana = usuario("ana@oto.com");
        bruno = usuario("bruno@oto.com");
    }

    private TokenUsuario token(Usuario dono, String hash, boolean usado, Instant expiraEm) {
        TokenUsuario t = new TokenUsuario(dono.getId(), TipoToken.CONVITE, hash, expiraEm);
        if (usado) {
            t.marcarUsado();
        }
        return gravar(t);
    }

    @Test
    void buscaPeloHashDoToken() {
        usuarios();
        token(ana, "hash-a", false, AMANHA);
        token(bruno, "hash-b", false, AMANHA);
        gravarELimpar();

        assertThat(repository.findByTokenHash("hash-b").orElseThrow().getUsuarioId()).isEqualTo(bruno.getId());
        assertThat(repository.findByTokenHash("hash-x")).isEmpty();
    }

    @Test
    void tokensPendentesDoUsuarioIgnoramUsadosEOsDeOutrosUsuarios() {
        usuarios();
        token(ana, "a-pendente", false, AMANHA);
        token(ana, "a-usado", true, AMANHA);
        token(bruno, "b-pendente", false, AMANHA);
        gravarELimpar();

        assertThat(repository.findByUsuarioIdAndUsadoEmIsNull(ana.getId())).extracting(TokenUsuario::getUsadoEm)
                .containsOnlyNulls().hasSize(1);
        assertThat(repository.findByUsuarioIdAndUsadoEmIsNull(ana.getId()).get(0).getId())
                .isEqualTo(repository.findByTokenHash("a-pendente").orElseThrow().getId());
    }

    @Test
    void tokenPendenteVencidoAindaApareceEntreOsNaoUsados() {
        usuarios();
        token(ana, "a-vencido", false, Instant.now().minusSeconds(60));
        gravarELimpar();

        assertThat(repository.findByUsuarioIdAndUsadoEmIsNull(ana.getId())).hasSize(1);
    }

    @Test
    void hashRepetidoViolaAUnicidade() {
        usuarios();
        token(ana, "mesmo-hash", false, AMANHA);

        assertThatThrownBy(() -> token(bruno, "mesmo-hash", false, AMANHA))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("uk_token_usuario_hash");
    }
}
