package OTO.auth;

import java.util.Locale;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.auth.AuthDtos.DefinirSenhaRequest;
import OTO.auth.AuthDtos.LoginRequest;
import OTO.auth.AuthDtos.LoginResponse;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.CredenciaisInvalidasException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.TokenJwtService;
import OTO.comum.seguranca.TokenJwtService.TokenEmitido;
import OTO.usuario.Usuario;
import OTO.usuario.UsuarioRepository;
import OTO.usuario.UsuarioResponse;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final TokenJwtService tokenJwt;
    private final TokenAcessoService tokensAcesso;
    private final AuditoriaService auditoria;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder senhas, TokenJwtService tokenJwt,
            TokenAcessoService tokensAcesso, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokenJwt = tokenJwt;
        this.tokensAcesso = tokensAcesso;
        this.auditoria = auditoria;
    }

    @Transactional
    public LoginResponse login(LoginRequest requisicao) {
        Usuario usuario = usuarios.findByEmail(requisicao.email().trim().toLowerCase(Locale.ROOT))
                .filter(Usuario::isAtivo)
                .filter(u -> u.getSenhaHash() != null && senhas.matches(requisicao.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        usuario.registrarAcesso();
        TokenEmitido token = tokenJwt.emitir(usuario);
        return new LoginResponse(token.token(), token.expiraEm(), UsuarioResponse.de(usuario));
    }

    /** primeiro acesso (convite) ou redefinição de senha: os dois usam o mesmo fluxo. */
    @Transactional
    public void definirSenha(DefinirSenhaRequest requisicao) {
        TokenUsuario token = tokensAcesso.validar(requisicao.token());
        Usuario usuario = usuarios.findById(token.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Usuário inativo. Fale com o OTO."));
        usuario.definirSenha(senhas.encode(requisicao.novaSenha()));
        token.marcarUsado();
        auditoria.registrar(usuario.getId(), "usuario", usuario.getId(), "DEFINIR_SENHA", null,
                Map.of("tipo", token.getTipo().name()));
    }
}
