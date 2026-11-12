package OTO.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class CriadorAdminInicialTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder senhas = mock(PasswordEncoder.class);

    private void rodar(String nome, String email, String senha) {
        new CriadorAdminInicial(new AdminInicialProperties(nome, email, senha), usuarios, senhas)
                .run(new DefaultApplicationArguments());
    }

    @Test
    void criaOAdministradorComOEmailNormalizado() {
        when(senhas.encode("senhaForte")).thenReturn("hash");

        rodar("Equipe OTO", "  Admin@OTO.com ", "senhaForte");

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("admin@oto.com");
        assertThat(salvo.getValue().getNome()).isEqualTo("Equipe OTO");
        assertThat(salvo.getValue().getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("hash");
        assertThat(salvo.getValue().isAtivo()).isTrue();
    }

    @Test
    void semNomeUsaONomePadrao() {
        when(senhas.encode(any())).thenReturn("hash");
        rodar("  ", "admin@oto.com", "senhaForte");
        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(salvo.capture());
        assertThat(salvo.getValue().getNome()).isEqualTo("Administrador OTO");
    }

    @Test
    void semEmailNaoCriaNada() {
        rodar("Equipe", null, "senhaForte");
        rodar("Equipe", "  ", "senhaForte");
        verify(usuarios, never()).save(any());
        verify(usuarios, never()).existsByEmail(any());
    }

    @Test
    void naoRecriaQuemJaExiste() {
        when(usuarios.existsByEmail("admin@oto.com")).thenReturn(true);
        rodar("Equipe", "admin@oto.com", "senhaForte");
        verify(usuarios, never()).save(any());
    }

    @Test
    void senhaCurtaOuAusenteNaoCria() {
        rodar("Equipe", "admin@oto.com", "1234567");
        rodar("Equipe", "admin@oto.com", null);
        verify(usuarios, never()).save(any());
    }
}
