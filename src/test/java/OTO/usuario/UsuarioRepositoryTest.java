package OTO.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.estabelecimento.Estabelecimento;

class UsuarioRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private UsuarioRepository repository;

    private Usuario usuario(String nome, String email, Perfil perfil) {
        return gravar(new Usuario(nome, email, perfil));
    }

    @Test
    void buscaPorEmailExato() {
        usuario("Ana", "ana@oto.com", Perfil.ADMIN);
        usuario("Bruno", "bruno@oto.com", Perfil.PARCEIRO);
        gravarELimpar();

        assertThat(repository.findByEmail("bruno@oto.com").orElseThrow().getNome()).isEqualTo("Bruno");
        assertThat(repository.findByEmail("carla@oto.com")).isEmpty();
    }

    @Test
    void existePorEmailSoParaOEmailGravado() {
        usuario("Ana", "ana@oto.com", Perfil.ADMIN);
        gravarELimpar();

        assertThat(repository.existsByEmail("ana@oto.com")).isTrue();
        assertThat(repository.existsByEmail("ana@oto.com.br")).isFalse();
    }

    @Test
    void listaPorNomeEmOrdemAlfabetica() {
        usuario("Carla", "carla@oto.com", Perfil.VISUALIZADOR);
        usuario("Ana", "ana@oto.com", Perfil.ADMIN);
        usuario("Bruno", "bruno@oto.com", Perfil.PARCEIRO);
        gravarELimpar();

        assertThat(repository.findAllByOrderByNome()).extracting(Usuario::getNome)
                .containsExactly("Ana", "Bruno", "Carla");
    }

    @Test
    void idsDosEstabelecimentosSaoSoDoUsuarioPedido() {
        Estabelecimento alfa = estabelecimento("Alfa", "HOTEL", "11111111000101");
        Estabelecimento beta = estabelecimento("Beta", "HOTEL", "11111111000102");
        Estabelecimento gama = estabelecimento("Gama", "HOTEL", "11111111000103");
        Usuario parceiro = usuario("Bruno", "bruno@oto.com", Perfil.PARCEIRO);
        Usuario outro = usuario("Carla", "carla@oto.com", Perfil.PARCEIRO);
        Usuario semEstabelecimento = usuario("Ana", "ana@oto.com", Perfil.ADMIN);
        parceiro.substituirEstabelecimentos(Set.of(alfa, beta));
        outro.substituirEstabelecimentos(Set.of(gama));
        gravarELimpar();

        assertThat(repository.findEstabelecimentoIds(parceiro.getId())).containsExactlyInAnyOrder(alfa.getId(),
                beta.getId());
        assertThat(repository.findEstabelecimentoIds(outro.getId())).containsExactly(gama.getId());
        assertThat(repository.findEstabelecimentoIds(semEstabelecimento.getId())).isEmpty();
    }

    @Test
    void emailRepetidoViolaAUnicidade() {
        usuario("Ana", "ana@oto.com", Perfil.ADMIN);

        assertThatThrownBy(() -> usuario("Outra Ana", "ana@oto.com", Perfil.PARCEIRO))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("uk_usuario_email");
    }

    @Test
    void emailComMaiusculasEhRejeitadoPeloBanco() {
        assertThatThrownBy(() -> usuario("Ana", "Ana@OTO.com", Perfil.ADMIN))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_usuario_email_minusculo");
    }
}
