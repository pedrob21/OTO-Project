package OTO.inventario;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.InventarioRequest;
import OTO.inventario.InventarioDtos.InventarioResponse;
import jakarta.validation.Valid;

/** Serviços, equipamentos, acessibilidade e indicadores ODS dos estabelecimentos. */
@RestController
public class InventarioController {

    private final InventarioService servico;

    public InventarioController(InventarioService servico) {
        this.servico = servico;
    }

    /** Perguntas do inventário (grupos e itens), para montar o formulário. */
    @GetMapping("/api/inventario/catalogo")
    public List<CatalogoGrupo> catalogo() {
        return servico.catalogo();
    }

    @GetMapping("/api/estabelecimentos/{id}/inventario")
    public InventarioResponse buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.buscar(id, UsuarioLogado.de(jwt));
    }

    @PutMapping("/api/estabelecimentos/{id}/inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARCEIRO')")
    public InventarioResponse salvar(@PathVariable Long id, @Valid @RequestBody InventarioRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.salvar(id, requisicao, UsuarioLogado.de(jwt));
    }
}
