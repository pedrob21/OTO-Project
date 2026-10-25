package OTO.comum.seguranca;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import OTO.usuario.Usuario;

/** emite o token de acesso devolvido no login. */
@Service
public class TokenJwtService {

    private final JwtEncoder encoder;
    private final JwtProperties propriedades;

    public TokenJwtService(JwtEncoder encoder, JwtProperties propriedades) {
        this.encoder = encoder;
        this.propriedades = propriedades;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(propriedades.expiracao());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("oto-api")
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(String.valueOf(usuario.getId()))
                .claim("perfil", usuario.getPerfil().name())
                .claim("nome", usuario.getNome())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }
}
