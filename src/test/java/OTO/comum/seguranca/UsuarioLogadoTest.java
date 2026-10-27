package OTO.comum.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;
import OTO.usuario.Perfil;

class UsuarioLogadoTest {

    @Test
    void leOIdEOPerfilDoToken() {
        UsuarioLogado usuario = UsuarioLogado.de(Fixtures.jwt(7, Perfil.PARCEIRO));
        assertThat(usuario.id()).isEqualTo(7L);
        assertThat(usuario.perfil()).isEqualTo(Perfil.PARCEIRO);
    }

    @Test
    void indicaOPerfil() {
        assertThat(Fixtures.ADMIN.isAdmin()).isTrue();
        assertThat(Fixtures.ADMIN.isParceiro()).isFalse();
        assertThat(Fixtures.PARCEIRO.isParceiro()).isTrue();
        assertThat(Fixtures.PARCEIRO.isVisualizador()).isFalse();
        assertThat(Fixtures.VISUALIZADOR.isVisualizador()).isTrue();
        assertThat(Fixtures.VISUALIZADOR.isAdmin()).isFalse();
    }
}
