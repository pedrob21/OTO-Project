package OTO.calculo;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import OTO.calculo.ResultadoDtos.CampanhaPublicadaResponse;
import OTO.calculo.ResultadoDtos.PreviaResponse;
import OTO.calculo.ResultadoDtos.ResultadoResponse;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.TipoPeriodo;

/** prévia, publicação e consulta dos resultados das campanhas. */
@RestController
@RequestMapping("/api/campanhas")
public class ResultadoController {

    private final ResultadoService servico;

    public ResultadoController(ResultadoService servico) {
        this.servico = servico;
    }

    /** Simulação com as respostas atuais e o que falta para poder publicar. */
    @GetMapping("/{id}/previa")
    @PreAuthorize("hasRole('ADMIN')")
    public PreviaResponse previa(@PathVariable Long id) {
        return servico.previa(id);
    }

    /** Calcula e grava o resultado. Exige a pesquisa encerrada e o mínimo de leitos respondentes. */
    @PostMapping("/{id}/publicacao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultadoResponse publicar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.publicar(id, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}/publicacao")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarPublicacao(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.cancelarPublicacao(id, UsuarioLogado.de(jwt));
    }

    @GetMapping("/{id}/resultado")
    public ResultadoResponse resultado(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return servico.resultado(id, UsuarioLogado.de(jwt));
    }

    @GetMapping("/{id}/resultado.csv")
    public ResponseEntity<byte[]> resultadoCsv(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        byte[] conteudo = servico.csv(id, UsuarioLogado.de(jwt));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("resultado-campanha-" + id + ".csv").build().toString())
                .body(conteudo);
    }

    /** Campanhas publicadas (mais recentes primeiro), com filtros opcionais. */
    @GetMapping("/publicadas")
    public List<CampanhaPublicadaResponse> publicadas(@RequestParam(required = false) Modalidade modalidade,
            @RequestParam(required = false) TipoPeriodo tipoPeriodo, @RequestParam(required = false) Integer ano) {
        return servico.publicadas(modalidade, tipoPeriodo, ano);
    }
}
