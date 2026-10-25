package OTO.pesquisa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.calculo.ResultadoCampanhaRepository;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.pesquisa.ParametroCalculoDtos.ParametroRequest;
import OTO.pesquisa.ParametroCalculoDtos.ParametroResponse;

/**
 * Parâmetros da metodologia (day use, permanência, gastos e mínimo para publicar).
 * Cada mudança é uma nova vigência, e cada resultado publicado guarda qual vigência
 * usou. Uma vigência ainda não usada em publicação pode ser corrigida ou excluída.
 */
@Service
public class ParametroCalculoService {

    private final ParametroCalculoRepository parametros;
    private final ResultadoCampanhaRepository resultados;
    private final AuditoriaService auditoria;

    public ParametroCalculoService(ParametroCalculoRepository parametros, ResultadoCampanhaRepository resultados,
            AuditoriaService auditoria) {
        this.parametros = parametros;
        this.resultados = resultados;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ParametroResponse> listar() {
        return parametros.findAllByOrderByVigenteDesdeDesc().stream().map(ParametroResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ParametroResponse vigente(LocalDate data) {
        return parametros.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(data)
                .map(ParametroResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Não há parâmetros de cálculo vigentes em " + data + "."));
    }

    @Transactional
    public ParametroResponse registrar(ParametroRequest r, UsuarioLogado admin) {
        if (r.diasEmParques() > r.diasPermanenciaMedia()) {
            throw new RegraNegocioException("Os dias em parques não podem passar a permanência média.");
        }
        if (parametros.existsByVigenteDesde(r.vigenteDesde())) {
            throw new RegraNegocioException("Já existem parâmetros a partir desta data. Use outra data.");
        }
        ParametroCalculo novo = parametros.save(new ParametroCalculo(r.vigenteDesde(), r.percentualDayUse(),
                r.diasPermanenciaMedia(), r.diasEmParques(), r.gastoMedioDiario(), r.gastoMedioIngresso(),
                r.percentualMinimoLeitosPublicacao(), r.fonte().trim(), admin.id()));
        auditoria.registrar(admin.id(), "parametro_calculo", novo.getId(), "CRIAR", null, r);
        return ParametroResponse.de(novo);
    }

    /** Corrige uma vigência ainda não usada em resultado publicado (as usadas ficam como estão). */
    @Transactional
    public ParametroResponse alterar(Long id, ParametroRequest r, UsuarioLogado admin) {
        ParametroCalculo parametro = parametros.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Parâmetro de cálculo", id));
        if (resultados.existsByParametroCalculoId(id)) {
            throw new RegraNegocioException(
                    "Estes parâmetros já foram usados em resultados publicados. Cadastre uma nova vigência.");
        }
        if (r.diasEmParques() > r.diasPermanenciaMedia()) {
            throw new RegraNegocioException("Os dias em parques não podem passar a permanência média.");
        }
        if (!parametro.getVigenteDesde().equals(r.vigenteDesde()) && parametros.existsByVigenteDesde(r.vigenteDesde())) {
            throw new RegraNegocioException("Já existem parâmetros a partir desta data. Use outra data.");
        }
        ParametroResponse antes = ParametroResponse.de(parametro);
        parametro.alterar(r.vigenteDesde(), r.percentualDayUse(), r.diasPermanenciaMedia(), r.diasEmParques(),
                r.gastoMedioDiario(), r.gastoMedioIngresso(), r.percentualMinimoLeitosPublicacao(), r.fonte().trim(),
                admin.id());
        ParametroResponse depois = ParametroResponse.de(parametro);
        auditoria.registrar(admin.id(), "parametro_calculo", id, "ATUALIZAR", antes, depois);
        return depois;
    }

    /** Só para corrigir um cadastro errado: não exclui parâmetros já usados em resultados publicados. */
    @Transactional
    public void excluir(Long id, UsuarioLogado admin) {
        ParametroCalculo parametro = parametros.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Parâmetro de cálculo", id));
        if (resultados.existsByParametroCalculoId(id)) {
            throw new RegraNegocioException("Estes parâmetros já foram usados em resultados publicados e não podem ser excluídos.");
        }
        if (parametros.count() == 1) {
            throw new RegraNegocioException("Não é possível excluir a única vigência de parâmetros.");
        }
        parametros.delete(parametro);
        auditoria.registrar(admin.id(), "parametro_calculo", id, "EXCLUIR", ParametroResponse.de(parametro), null);
    }
}
