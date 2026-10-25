package OTO.inventario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.CatalogoItem;
import OTO.inventario.InventarioDtos.GrupoRequest;
import OTO.inventario.InventarioDtos.ItemRequest;
import jakarta.validation.Valid;

/** Manutenção do catálogo do inventário (grupos e itens) pela equipe do OTO. */
@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogoInventarioController {

    private final CatalogoInventarioService servico;

    public CatalogoInventarioController(CatalogoInventarioService servico) {
        this.servico = servico;
    }

    /** Catálogo inteiro, inclusive grupos e itens inativos (o GET /catalogo traz só os ativos). */
    @GetMapping("/catalogo/completo")
    public List<CatalogoGrupo> completo() {
        return servico.completo();
    }

    @PostMapping("/grupos")
    public ResponseEntity<CatalogoGrupo> criarGrupo(@Valid @RequestBody GrupoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criarGrupo(requisicao, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/grupos/{id}")
    public CatalogoGrupo alterarGrupo(@PathVariable Long id, @Valid @RequestBody GrupoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterarGrupo(id, requisicao, UsuarioLogado.de(jwt));
    }

    @PostMapping("/grupos/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativarGrupo(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivoGrupo(id, false, UsuarioLogado.de(jwt));
    }

    @PostMapping("/grupos/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativarGrupo(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivoGrupo(id, true, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/grupos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirGrupo(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.excluirGrupo(id, UsuarioLogado.de(jwt));
    }

    @PostMapping("/grupos/{grupoId}/itens")
    public ResponseEntity<CatalogoItem> criarItem(@PathVariable Long grupoId, @Valid @RequestBody ItemRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servico.criarItem(grupoId, requisicao, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/itens/{id}")
    public CatalogoItem alterarItem(@PathVariable Long id, @Valid @RequestBody ItemRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterarItem(id, requisicao, UsuarioLogado.de(jwt));
    }

    @PostMapping("/itens/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativarItem(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivoItem(id, false, UsuarioLogado.de(jwt));
    }

    @PostMapping("/itens/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativarItem(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivoItem(id, true, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/itens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirItem(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.excluirItem(id, UsuarioLogado.de(jwt));
    }
}
