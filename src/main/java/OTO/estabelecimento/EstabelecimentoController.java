package OTO.estabelecimento;

import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.EstabelecimentoDtos.AtualizacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeResponse;
import OTO.estabelecimento.EstabelecimentoDtos.CriacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResponse;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResumo;
import jakarta.validation.Valid;

/** Meios de hospedagem do inventário. O PARCEIRO só vê e altera os estabelecimentos vinculados a ele. */
@RestController
@RequestMapping("/api/estabelecimentos")
public class EstabelecimentoController {

    private final EstabelecimentoService servico;

    public EstabelecimentoController(EstabelecimentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<EstabelecimentoResumo> listar(@RequestParam(required = false) String tipo,
            @RequestParam(required = false) Boolean ativo, @AuthenticationPrincipal Jwt jwt) {
        return servico.listar(UsuarioLogado.de(jwt), tipo, ativo);
    }

    @GetMapping("/{id}")
    public EstabelecimentoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.buscar(id, UsuarioLogado.de(jwt));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EstabelecimentoResponse> criar(@Valid @RequestBody CriacaoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        EstabelecimentoResponse criado = servico.criar(requisicao, UsuarioLogado.de(jwt));
        return ResponseEntity.created(URI.create("/api/estabelecimentos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARCEIRO')")
    public EstabelecimentoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizacaoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.atualizar(id, requisicao, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivo(id, false, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.definirAtivo(id, true, UsuarioLogado.de(jwt));
    }

    /** Histórico de capacidade (UH e leitos), da vigência mais recente para a mais antiga. */
    @GetMapping("/{id}/capacidades")
    public List<CapacidadeResponse> capacidades(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.capacidades(id, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/capacidades")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARCEIRO')")
    public ResponseEntity<CapacidadeResponse> registrarCapacidade(@PathVariable Long id,
            @Valid @RequestBody CapacidadeRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servico.registrarCapacidade(id, requisicao, UsuarioLogado.de(jwt)));
    }

    /** Correção de um lançamento digitado errado (para mudança real de capacidade, use o POST). */
    @PutMapping("/{id}/capacidades/{capacidadeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CapacidadeResponse corrigirCapacidade(@PathVariable Long id, @PathVariable Long capacidadeId,
            @Valid @RequestBody CapacidadeRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return servico.corrigirCapacidade(id, capacidadeId, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}/capacidades/{capacidadeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCapacidade(@PathVariable Long id, @PathVariable Long capacidadeId,
            @AuthenticationPrincipal Jwt jwt) {
        servico.excluirCapacidade(id, capacidadeId, UsuarioLogado.de(jwt));
    }
}
