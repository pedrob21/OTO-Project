package OTO.comum.seguranca;

import org.springframework.security.oauth2.jwt.Jwt;

import OTO.usuario.Perfil;

/** usuário da requisição, extraído do token. Nos controllers: {@code @AuthenticationPrincipal Jwt jwt}. */
public record UsuarioLogado(Long id, Perfil perfil) {

    public static UsuarioLogado de(Jwt jwt) {
        return new UsuarioLogado(Long.valueOf(jwt.getSubject()), Perfil.valueOf(jwt.getClaimAsString("perfil")));
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }

    public boolean isParceiro() {
        return perfil == Perfil.PARCEIRO;
    }

    public boolean isVisualizador() {
        return perfil == Perfil.VISUALIZADOR;
    }
}
