package OTO.usuario;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import OTO.auth.LinkAcesso;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.usuario.UsuarioDtos.UsuarioAlteracaoRequest;
import OTO.usuario.UsuarioDtos.UsuarioCriadoResponse;
import OTO.usuario.UsuarioDtos.UsuarioRequest;
import jakarta.validation.Valid;

/** Gestão de usuários (somente ADMIN). */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService servico;

    public UsuarioController(UsuarioService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioCriadoResponse> criar(@Valid @RequestBody UsuarioRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        UsuarioCriadoResponse criado = servico.criar(requisicao, UsuarioLogado.de(jwt));
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.usuario().id())).body(criado);
    }

    @PutMapping("/{id}")
    public UsuarioResponse alterar(@PathVariable Long id, @Valid @RequestBody UsuarioAlteracaoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterar(id, requisicao, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivo(id, false, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivo(id, true, UsuarioLogado.de(jwt));
    }

    /** Gera um novo link de acesso (convite ou redefinição de senha) para o admin enviar. */
    @PostMapping("/{id}/link-de-senha")
    public LinkAcesso gerarLinkDeSenha(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.gerarLinkDeSenha(id, UsuarioLogado.de(jwt));
    }
}
