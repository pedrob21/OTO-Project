package OTO.pesquisa;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.pesquisa.DiariaInformadaDtos.DiariaInformada;
import OTO.pesquisa.DiariaInformadaDtos.DiariaInformadaResponse;
import OTO.pesquisa.DiariaInformadaDtos.DiariasRequest;
import OTO.referencia.TipoHospedagemRepository;

/**
 * Diárias médias digitadas pela equipe para uma campanha (como as células de diária da
 * planilha de tabulação). Valem no cálculo no lugar da média das respostas.
 */
@Service
public class DiariaInformadaService {

    private static final String GERAL = "";

    private final CampanhaRepository campanhas;
    private final CampanhaDiariaRepository diarias;
    private final TipoHospedagemRepository tipos;
    private final AuditoriaService auditoria;

    public DiariaInformadaService(CampanhaRepository campanhas, CampanhaDiariaRepository diarias,
            TipoHospedagemRepository tipos, AuditoriaService auditoria) {
        this.campanhas = campanhas;
        this.diarias = diarias;
        this.tipos = tipos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<DiariaInformadaResponse> listar(Long campanhaId) {
        carregar(campanhaId);
        return ordenadas(diarias.findByCampanhaId(campanhaId));
    }

    /** Substitui as diárias digitadas da campanha. Só antes da publicação. */
    @Transactional
    public List<DiariaInformadaResponse> salvar(Long campanhaId, DiariasRequest requisicao, UsuarioLogado admin) {
        Campanha campanha = carregar(campanhaId);
        if (campanha.isPublicada()) {
            throw new RegraNegocioException("A campanha já foi publicada. Cancele a publicação para mudar as diárias.");
        }
        String fonte = requisicao.fonte() == null || requisicao.fonte().isBlank() ? null : requisicao.fonte().trim();
        Map<String, BigDecimal> pedidas = new HashMap<>();
        for (DiariaInformada d : requisicao.diarias()) {
            String tipo = d.tipoHospedagemCodigo() == null || d.tipoHospedagemCodigo().isBlank()
                    ? null : d.tipoHospedagemCodigo().trim();
            if (tipo != null && !tipos.existsById(tipo)) {
                throw new RegraNegocioException("Tipo de hospedagem inválido: " + tipo + ". Veja /api/referencias.");
            }
            if (pedidas.put(Objects.requireNonNullElse(tipo, GERAL), d.diariaMedia()) != null) {
                throw new RegraNegocioException(tipo == null
                        ? "A diária geral foi enviada mais de uma vez."
                        : "A diária de " + tipo + " foi enviada mais de uma vez.");
            }
        }

        List<CampanhaDiaria> atuais = diarias.findByCampanhaId(campanhaId);
        List<DiariaInformadaResponse> antes = ordenadas(atuais);
        Set<String> mantidas = new HashSet<>();
        for (CampanhaDiaria atual : atuais) {
            String chave = Objects.requireNonNullElse(atual.getTipoHospedagemCodigo(), GERAL);
            BigDecimal valor = pedidas.get(chave);
            if (valor == null) {
                diarias.delete(atual);
            }
            else {
                atual.alterar(valor, fonte, admin.id());
                mantidas.add(chave);
            }
        }
        pedidas.forEach((chave, valor) -> {
            if (!mantidas.contains(chave)) {
                diarias.save(new CampanhaDiaria(campanhaId, GERAL.equals(chave) ? null : chave, valor, fonte, admin.id()));
            }
        });

        List<DiariaInformadaResponse> depois = ordenadas(diarias.findByCampanhaId(campanhaId));
        auditoria.registrar(admin.id(), "campanha_diaria", campanhaId, "ATUALIZAR", Map.of("diarias", antes),
                Map.of("diarias", depois));
        return depois;
    }

    private static List<DiariaInformadaResponse> ordenadas(List<CampanhaDiaria> lista) {
        return lista.stream()
                .sorted(Comparator.comparing(CampanhaDiaria::getTipoHospedagemCodigo,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(DiariaInformadaResponse::de)
                .toList();
    }

    private Campanha carregar(Long id) {
        return campanhas.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Campanha", id));
    }
}
