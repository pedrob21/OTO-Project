package OTO.pesquisa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.pesquisa.ParametroCalculoDtos.ParametroRequest;
import OTO.pesquisa.ParametroCalculoDtos.ParametroResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/parametros-calculo")
public class ParametroCalculoController {

    private final ParametroCalculoService servico;

    public ParametroCalculoController(ParametroCalculoService servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VISUALIZADOR')")
    public List<ParametroResponse> listar() {
        return servico.listar();
    }

    /** Parâmetros vigentes na data (padrão: hoje). Usados nas notas metodológicas dos boletins. */
    @GetMapping("/vigente")
    public ParametroResponse vigente(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return servico.vigente(data == null ? LocalDate.now() : data);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParametroResponse> registrar(@Valid @RequestBody ParametroRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.registrar(requisicao, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ParametroResponse alterar(@PathVariable Long id, @Valid @RequestBody ParametroRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterar(id, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.excluir(id, UsuarioLogado.de(jwt));
    }
}
