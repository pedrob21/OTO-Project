package OTO.estabelecimento;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.EstabelecimentoDtos.RhcRequest;
import OTO.estabelecimento.EstabelecimentoDtos.RhcResponse;

/** Totais de hospedagem caseira (RHC), com histórico por vigência. */
@Service
public class RhcService {

    private final RhcResumoRepository rhc;
    private final AuditoriaService auditoria;

    public RhcService(RhcResumoRepository rhc, AuditoriaService auditoria) {
        this.rhc = rhc;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<RhcResponse> listar() {
        return rhc.findAllByOrderByVigenteDesdeDesc().stream().map(RhcResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public RhcResponse vigente(LocalDate data) {
        return rhc.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(data)
                .map(RhcResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Não há totais de RHC vigentes em " + data + "."));
    }

    @Transactional
    public RhcResponse registrar(RhcRequest requisicao, UsuarioLogado admin) {
        if (rhc.existsByVigenteDesde(requisicao.vigenteDesde())) {
            throw new RegraNegocioException("Já existem totais de RHC a partir desta data. Use outra data.");
        }
        RhcResumo novo = rhc.save(new RhcResumo(requisicao.vigenteDesde(), requisicao.qtdRegistros(),
                requisicao.qtdUh(), requisicao.qtdLeitos(), requisicao.maxLeitosPorUh(), admin.id()));
        auditoria.registrar(admin.id(), "rhc_resumo", novo.getId(), "CRIAR", null, requisicao);
        return RhcResponse.de(novo);
    }

    /** Corrige um lançamento digitado errado. Resultados já publicados não mudam. */
    @Transactional
    public RhcResponse corrigir(Long id, RhcRequest requisicao, UsuarioLogado admin) {
        RhcResumo atual = carregar(id);
        if (!atual.getVigenteDesde().equals(requisicao.vigenteDesde()) && rhc.existsByVigenteDesde(requisicao.vigenteDesde())) {
            throw new RegraNegocioException("Já existem totais de RHC a partir desta data. Use outra data.");
        }
        RhcResponse antes = RhcResponse.de(atual);
        atual.corrigir(requisicao.vigenteDesde(), requisicao.qtdRegistros(), requisicao.qtdUh(), requisicao.qtdLeitos(),
                requisicao.maxLeitosPorUh(), admin.id());
        RhcResponse depois = RhcResponse.de(atual);
        auditoria.registrar(admin.id(), "rhc_resumo", id, "CORRIGIR", antes, depois);
        return depois;
    }

    @Transactional
    public void excluir(Long id, UsuarioLogado admin) {
        RhcResumo atual = carregar(id);
        RhcResponse antes = RhcResponse.de(atual);
        rhc.delete(atual);
        auditoria.registrar(admin.id(), "rhc_resumo", id, "EXCLUIR", antes, null);
    }

    private RhcResumo carregar(Long id) {
        return rhc.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Totais de RHC", id));
    }
}
