package OTO.historico;

import java.io.IOException;
import java.util.List;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.historico.HistoricoDtos.HistoricoRequest;
import OTO.historico.HistoricoDtos.HistoricoResponse;
import OTO.historico.HistoricoDtos.ImportacaoResponse;
import OTO.pesquisa.Modalidade;
import jakarta.validation.Valid;

/** Série histórica de indicadores (valores anteriores ao sistema). */
@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService servico;

    public HistoricoController(HistoricoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<HistoricoResponse> listar(@RequestParam(required = false) Indicador indicador,
            @RequestParam(required = false) Modalidade modalidade, @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String tipo) {
        return servico.listar(indicador, modalidade, ano, tipo);
    }

    /** Grava um valor (ou substitui o da mesma combinação). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public HistoricoResponse salvar(@Valid @RequestBody HistoricoRequest requisicao, @AuthenticationPrincipal Jwt jwt) {
        return servico.salvar(requisicao, UsuarioLogado.de(jwt));
    }

    /** Corrige um valor (inclusive o indicador, o período e o tipo). */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HistoricoResponse alterar(@PathVariable Long id, @Valid @RequestBody HistoricoRequest requisicao,
            @AuthenticationPrincipal Jwt jwt) {
        return servico.alterar(id, requisicao, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        servico.excluir(id, UsuarioLogado.de(jwt));
    }

    /**
     * Importa um CSV (campo "arquivo") com a fonte dos dados (campo "fonte").
     * Responde 200 com as contagens ou 422 com os erros por linha (e não grava nada).
     */
    @PostMapping(value = "/importacao", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportacaoResponse> importar(@RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("fonte") String fonte, @AuthenticationPrincipal Jwt jwt) throws IOException {
        ImportacaoResponse resposta = servico.importar(arquivo.getBytes(), fonte, UsuarioLogado.de(jwt));
        return resposta.erros().isEmpty()
                ? ResponseEntity.ok(resposta)
                : ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(resposta);
    }
}
