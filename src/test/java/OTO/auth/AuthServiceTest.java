package OTO.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import OTO.Fixtures;
import OTO.auth.AuthDtos.DefinirSenhaRequest;
import OTO.auth.AuthDtos.LoginRequest;
import OTO.auth.AuthDtos.LoginResponse;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.CredenciaisInvalidasException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.TokenJwtService;
import OTO.comum.seguranca.TokenJwtService.TokenEmitido;
import OTO.usuario.Perfil;
import OTO.usuario.Usuario;
import OTO.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private PasswordEncoder senhas;
    @Mock
    private TokenJwtService tokenJwt;
    @Mock
    private TokenAcessoService tokensAcesso;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private AuthService auth;

    private Usuario usuarioComSenha() {
        Usuario usuario = Fixtures.usuario(5, "Ana", "ana@oto.com", Perfil.ADMIN);
        usuario.definirSenha("hash");
        return usuario;
    }

    @Test
    void loginNormalizaOEmailEDevolveOToken() {
        Usuario usuario = usuarioComSenha();
        Instant expira = Instant.now().plusSeconds(3600);
        when(usuarios.findByEmail("ana@oto.com")).thenReturn(Optional.of(usuario));
        when(senhas.matches("segredo123", "hash")).thenReturn(true);
        when(tokenJwt.emitir(usuario)).thenReturn(new TokenEmitido("jwt", expira));

        LoginResponse resposta = auth.login(new LoginRequest("  Ana@OTO.com ", "segredo123"));

        assertThat(resposta.token()).isEqualTo("jwt");
        assertThat(resposta.expiraEm()).isEqualTo(expira);
        assertThat(resposta.usuario().id()).isEqualTo(5L);
        assertThat(usuario.getUltimoAcessoEm()).isNotNull();
    }

    @Test
    void emailDesconhecidoEhCredencialInvalida() {
        when(usuarios.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login(new LoginRequest("x@oto.com", "a")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void senhaErradaEhCredencialInvalida() {
        when(usuarios.findByEmail("ana@oto.com")).thenReturn(Optional.of(usuarioComSenha()));
        when(senhas.matches("errada", "hash")).thenReturn(false);
        assertThatThrownBy(() -> auth.login(new LoginRequest("ana@oto.com", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(tokenJwt, never()).emitir(any());
    }

    @Test
    void usuarioInativoNaoEntra() {
        Usuario usuario = usuarioComSenha();
        usuario.definirAtivo(false);
        when(usuarios.findByEmail("ana@oto.com")).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> auth.login(new LoginRequest("ana@oto.com", "segredo123")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(senhas, never()).matches(any(), any());
    }

    @Test
    void usuarioSemSenhaDefinidaNaoEntra() {
        when(usuarios.findByEmail("ana@oto.com"))
                .thenReturn(Optional.of(Fixtures.usuario(5, "Ana", "ana@oto.com", Perfil.ADMIN)));
        assertThatThrownBy(() -> auth.login(new LoginRequest("ana@oto.com", "qualquer")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(senhas, never()).matches(any(), any());
    }

    @Test
    void definirSenhaGravaOHashEInutilizaOToken() {
        TokenUsuario token = new TokenUsuario(5L, TipoToken.CONVITE, "h", Instant.now().plusSeconds(60));
        Usuario usuario = Fixtures.usuario(5, "Ana", "ana@oto.com", Perfil.PARCEIRO);
        when(tokensAcesso.validar("tk")).thenReturn(token);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(senhas.encode("novaSenha1")).thenReturn("novoHash");

        auth.definirSenha(new DefinirSenhaRequest("tk", "novaSenha1"));

        assertThat(usuario.getSenhaHash()).isEqualTo("novoHash");
        assertThat(token.getUsadoEm()).isNotNull();
        verify(auditoria).registrar(5L, "usuario", 5L, "DEFINIR_SENHA", null, Map.of("tipo", "CONVITE"));
    }

    @Test
    void usuarioInativoNaoDefineSenha() {
        TokenUsuario token = new TokenUsuario(5L, TipoToken.REDEFINICAO_SENHA, "h", Instant.now().plusSeconds(60));
        Usuario usuario = Fixtures.usuario(5, "Ana", "ana@oto.com", Perfil.PARCEIRO);
        usuario.definirAtivo(false);
        when(tokensAcesso.validar("tk")).thenReturn(token);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> auth.definirSenha(new DefinirSenhaRequest("tk", "novaSenha1")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");
        assertThat(token.getUsadoEm()).isNull();
        assertThat(usuario.getSenhaHash()).isNull();
    }
}
