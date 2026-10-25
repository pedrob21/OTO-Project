package OTO.estabelecimento;

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
import OTO.estabelecimento.EstabelecimentoDtos.RhcRequest;
import OTO.estabelecimento.EstabelecimentoDtos.RhcResponse;
import jakarta.validation.Valid;

/** Totais de Registros de Hospedagem Caseira. Só os totais entram no sistema (LGPD). */
@RestController
@RequestMapping("/api/rhc")
@PreAuthorize("hasAnyRole('ADMIN', 'VISUALIZADOR')")
public class RhcController {

    private final RhcService servico;

    public RhcController(RhcService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<RhcResponse> listar() {
        return servico.listar();
    }

    /** Totais vigentes na data (padrão: hoje). */
    @GetMapping("/vigente")
    public RhcResponse vigente(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return servico.vigente(data == null ? LocalDate.now() : data);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RhcResponse> registrar(@Valid @RequestBody RhcRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.registrar(requisicao, UsuarioLogado.de(jwt)));
    }

    /** Correção de um lançamento digitado errado. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RhcResponse corrigir(@PathVariable Long id, @Valid @RequestBody RhcRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.corrigir(id, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.excluir(id, UsuarioLogado.de(jwt));
    }
}
