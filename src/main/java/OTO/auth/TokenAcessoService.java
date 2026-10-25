package OTO.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.excecao.RegraNegocioException;

/** classe que gera e valida os tokens de uso único (convite e redefinição de senha). */
@Service
public class TokenAcessoService {

    private static final Duration VALIDADE_CONVITE = Duration.ofDays(7);
    private static final Duration VALIDADE_REDEFINICAO = Duration.ofHours(24);

    private final TokenUsuarioRepository tokens;
    private final SecureRandom aleatorio = new SecureRandom();

    public TokenAcessoService(TokenUsuarioRepository tokens) {
        this.tokens = tokens;
    }

    /** Gera um novo token e invalida os anteriores ainda não usados do mesmo usuário. */
    @Transactional
    public LinkAcesso gerar(Long usuarioId, TipoToken tipo) {
        tokens.findByUsuarioIdAndUsadoEmIsNull(usuarioId).forEach(TokenUsuario::marcarUsado);
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration validade = tipo == TipoToken.CONVITE ? VALIDADE_CONVITE : VALIDADE_REDEFINICAO;
        Instant expiraEm = Instant.now().plus(validade);
        tokens.save(new TokenUsuario(usuarioId, tipo, hash(token), expiraEm));
        return new LinkAcesso(token, tipo, expiraEm);
    }

    /** Devolve o token se ele existir, não tiver expirado e não tiver sido usado. */
    @Transactional(readOnly = true)
    public TokenUsuario validar(String token) {
        TokenUsuario encontrado = tokens.findByTokenHash(hash(token))
                .orElseThrow(() -> new RegraNegocioException("Link inválido. Peça um novo ao OTO."));
        if (!encontrado.isValido(Instant.now())) {
            throw new RegraNegocioException("Este link expirou ou já foi usado. Peça um novo ao OTO.");
        }
        return encontrado;
    }

    static String hash(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }
}
