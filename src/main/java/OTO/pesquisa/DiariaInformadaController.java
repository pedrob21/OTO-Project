package OTO.pesquisa;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.pesquisa.DiariaInformadaDtos.DiariaInformadaResponse;
import OTO.pesquisa.DiariaInformadaDtos.DiariasRequest;
import jakarta.validation.Valid;

/** Diárias médias que a equipe do OTO digita para a campanha (somente ADMIN). */
@RestController
@RequestMapping("/api/campanhas/{campanhaId}/diarias")
@PreAuthorize("hasRole('ADMIN')")
public class DiariaInformadaController {

    private final DiariaInformadaService servico;

    public DiariaInformadaController(DiariaInformadaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<DiariaInformadaResponse> listar(@PathVariable Long campanhaId) {
        return servico.listar(campanhaId);
    }

    @PutMapping
    public List<DiariaInformadaResponse> salvar(@PathVariable Long campanhaId, @Valid @RequestBody DiariasRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.salvar(campanhaId, requisicao, UsuarioLogado.de(jwt));
    }
}
