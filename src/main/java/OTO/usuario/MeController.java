package OTO.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.usuario.UsuarioDtos.TrocaSenhaRequest;
import jakarta.validation.Valid;

/** Dados do próprio usuário logado (qualquer perfil). */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UsuarioService servico;

    public MeController(UsuarioService servico) {
        this.servico = servico;
    }

    @GetMapping
    public UsuarioResponse meusDados(@AuthenticationPrincipal Jwt jwt) {
        return servico.meusDados(UsuarioLogado.de(jwt));
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@Valid @RequestBody TrocaSenhaRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        servico.trocarSenha(UsuarioLogado.de(jwt), requisicao);
    }
}
