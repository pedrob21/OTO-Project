package OTO.pesquisa;

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
import OTO.pesquisa.RespostaDtos.HistoricoRespostaResponse;
import OTO.pesquisa.RespostaDtos.MinhaRespostaResponse;
import OTO.pesquisa.RespostaDtos.PendenciaResponse;
import OTO.pesquisa.RespostaDtos.RespostaRequest;
import OTO.pesquisa.RespostaDtos.RespostaResponse;
import jakarta.validation.Valid;

/** Respostas das pesquisas de ocupação. O VISUALIZADOR não tem acesso a respostas individuais. */
@RestController
public class RespostaController {

    private final RespostaService servico;

    public RespostaController(RespostaService servico) {
        this.servico = servico;
    }

    /** Envia ou corrige a resposta (PUT idempotente: vale o último envio). */
    @PutMapping("/api/campanhas/{campanhaId}/respostas/{estabelecimentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARCEIRO')")
    public RespostaResponse responder(@PathVariable Long campanhaId, @PathVariable Long estabelecimentoId,
            @Valid @RequestBody RespostaRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return servico.responder(campanhaId, estabelecimentoId, requisicao, UsuarioLogado.de(jwt));
    }

    @GetMapping("/api/campanhas/{campanhaId}/respostas")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RespostaResponse> listar(@PathVariable Long campanhaId) {
        return servico.listarDaCampanha(campanhaId);
    }

    @GetMapping("/api/respostas/pendencias")
    @PreAuthorize("hasRole('PARCEIRO')")
    public List<PendenciaResponse> pendencias(@AuthenticationPrincipal Jwt jwt) {
        return servico.pendencias(UsuarioLogado.de(jwt));
    }

    @GetMapping("/api/respostas/minhas")
    @PreAuthorize("hasRole('PARCEIRO')")
    public List<MinhaRespostaResponse> minhas(@AuthenticationPrincipal Jwt jwt) {
        return servico.minhas(UsuarioLogado.de(jwt));
    }

    @GetMapping("/api/respostas/{id}/historico")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARCEIRO')")
    public List<HistoricoRespostaResponse> historico(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.historico(id, UsuarioLogado.de(jwt));
    }
}
