package OTO.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;
import OTO.usuario.UsuarioResponse.EstabelecimentoRef;

class UsuarioTest {

    @Test
    void novoUsuarioEstaAtivoESemSenha() {
        Usuario usuario = new Usuario("Ana", "ana@oto.com", Perfil.ADMIN);
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.isAdmin()).isTrue();
        assertThat(usuario.getSenhaHash()).isNull();
        assertThat(usuario.getCriadoEm()).isNotNull();
        assertThat(new Usuario("Bia", "bia@oto.com", Perfil.PARCEIRO).isAdmin()).isFalse();
    }

    @Test
    void substituirEstabelecimentosTrocaOsVinculos() {
        Usuario usuario = new Usuario("P", "p@x.com", Perfil.PARCEIRO);
        usuario.substituirEstabelecimentos(Set.of(Fixtures.estabelecimento(1, "A", "HOTEL")));
        usuario.substituirEstabelecimentos(Set.of(Fixtures.estabelecimento(2, "B", "HOTEL")));
        assertThat(usuario.getEstabelecimentos()).extracting(e -> e.getId()).containsExactly(2L);
    }

    @Test
    void respostaOrdenaEstabelecimentosPeloNomeEIndicaPrimeiroAcesso() {
        Usuario usuario = Fixtures.usuario(3, "P", "p@x.com", Perfil.PARCEIRO);
        usuario.substituirEstabelecimentos(Set.of(
                Fixtures.estabelecimento(1, "Pousada Sol", "POUSADA"),
                Fixtures.estabelecimento(2, "Hotel Azul", "HOTEL")));

        UsuarioResponse resposta = UsuarioResponse.de(usuario);

        assertThat(resposta.estabelecimentos())
                .containsExactly(new EstabelecimentoRef(2L, "Hotel Azul"), new EstabelecimentoRef(1L, "Pousada Sol"));
        assertThat(resposta.primeiroAcessoPendente()).isTrue();
        usuario.definirSenha("hash");
        assertThat(UsuarioResponse.de(usuario).primeiroAcessoPendente()).isFalse();
    }
}
