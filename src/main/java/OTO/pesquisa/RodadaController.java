package OTO.pesquisa;

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
import OTO.pesquisa.RodadaDtos.AberturaRequest;
import OTO.pesquisa.RodadaDtos.AcompanhamentoResponse;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.RodadaDtos.PrazoRequest;
import OTO.pesquisa.RodadaDtos.RodadaRequest;
import OTO.pesquisa.RodadaDtos.RodadaResponse;
import jakarta.validation.Valid;

/** Pesquisas de ocupação e suas campanhas (somente ADMIN). */
@RestController
@RequestMapping("/api/rodadas")
@PreAuthorize("hasRole('ADMIN')")
public class RodadaController {

    private final RodadaService servico;

    public RodadaController(RodadaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<RodadaResponse> listar(@RequestParam(required = false) StatusRodada status) {
        return servico.listar(status);
    }

    @GetMapping("/{id}")
    public RodadaResponse buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<RodadaResponse> criar(@Valid @RequestBody RodadaRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        RodadaResponse criada = servico.criar(requisicao, UsuarioLogado.de(jwt));
        return ResponseEntity.created(URI.create("/api/rodadas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public RodadaResponse alterar(@PathVariable Long id, @Valid @RequestBody RodadaRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterar(id, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.excluir(id, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/campanhas")
    public ResponseEntity<CampanhaResponse> adicionarCampanha(@PathVariable Long id,
            @Valid @RequestBody DadosCampanha dados, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servico.adicionarCampanha(id, dados, UsuarioLogado.de(jwt)));
    }

    @PutMapping("/{id}/campanhas/{campanhaId}")
    public CampanhaResponse alterarCampanha(@PathVariable Long id, @PathVariable Long campanhaId,
            @Valid @RequestBody DadosCampanha dados, @AuthenticationPrincipal Jwt jwt) {
        return servico.alterarCampanha(id, campanhaId, dados, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}/campanhas/{campanhaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCampanha(@PathVariable Long id, @PathVariable Long campanhaId, @AuthenticationPrincipal Jwt jwt) {
        servico.excluirCampanha(id, campanhaId, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/abrir")
    public RodadaResponse abrir(@PathVariable Long id, @Valid @RequestBody AberturaRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.abrir(id, requisicao, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/prorrogar")
    public RodadaResponse prorrogar(@PathVariable Long id, @Valid @RequestBody PrazoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.prorrogar(id, requisicao, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/encerrar")
    public RodadaResponse encerrar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.encerrar(id, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/reabrir")
    public RodadaResponse reabrir(@PathVariable Long id, @Valid @RequestBody PrazoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.reabrir(id, requisicao, UsuarioLogado.de(jwt));
    }

    /** Quem respondeu e quem falta em cada campanha, com os contatos para cobrança. */
    @GetMapping("/{id}/acompanhamento")
    public AcompanhamentoResponse acompanhamento(@PathVariable Long id) {
        return servico.acompanhamento(id);
    }
}
