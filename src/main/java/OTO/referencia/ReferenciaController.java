package OTO.referencia;

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
import OTO.referencia.ReferenciaDtos.FaixaRef;
import OTO.referencia.ReferenciaDtos.FaixaRequest;
import OTO.referencia.ReferenciaDtos.FeriadoRef;
import OTO.referencia.ReferenciaDtos.FeriadoRequest;
import OTO.referencia.ReferenciaDtos.Referencias;
import OTO.referencia.ReferenciaDtos.TipoRef;
import OTO.referencia.ReferenciaDtos.TipoRequest;
import jakarta.validation.Valid;

/** Listas de referência para os formulários e filtros do front, mantidas pela equipe do OTO. */
@RestController
@RequestMapping("/api/referencias")
public class ReferenciaController {

    private final ReferenciaService servico;

    public ReferenciaController(ReferenciaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public Referencias listar() {
        return servico.listar();
    }

    @PostMapping("/tipos-hospedagem")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoRef> criarTipo(@Valid @RequestBody TipoRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criarTipo(requisicao, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/tipos-hospedagem/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public TipoRef alterarTipo(@PathVariable String codigo, @Valid @RequestBody TipoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterarTipo(codigo, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/tipos-hospedagem/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirTipo(@PathVariable String codigo, @AuthenticationPrincipal Jwt jwt) {
        servico.excluirTipo(codigo, UsuarioLogado.de(jwt));
    }

    @PostMapping("/feriados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeriadoRef> criarFeriado(@Valid @RequestBody FeriadoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criarFeriado(requisicao, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/feriados/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public FeriadoRef alterarFeriado(@PathVariable String codigo, @Valid @RequestBody FeriadoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterarFeriado(codigo, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/feriados/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFeriado(@PathVariable String codigo, @AuthenticationPrincipal Jwt jwt) {
        servico.excluirFeriado(codigo, UsuarioLogado.de(jwt));
    }

    @PostMapping("/faixas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FaixaRef> criarFaixa(@Valid @RequestBody FaixaRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criarFaixa(requisicao, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/faixas/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public FaixaRef alterarFaixa(@PathVariable String codigo, @Valid @RequestBody FaixaRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterarFaixa(codigo, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/faixas/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFaixa(@PathVariable String codigo, @AuthenticationPrincipal Jwt jwt) {
        servico.excluirFaixa(codigo, UsuarioLogado.de(jwt));
    }
}
