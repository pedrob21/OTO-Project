package OTO.estabelecimento;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.ControleAcesso;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.EstabelecimentoDtos.AtualizacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeResponse;
import OTO.estabelecimento.EstabelecimentoDtos.CriacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResponse;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResumo;
import OTO.referencia.TipoHospedagemRepository;

@Service
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentos;
    private final CapacidadeHospedagemRepository capacidades;
    private final TipoHospedagemRepository tipos;
    private final ControleAcesso acesso;
    private final AuditoriaService auditoria;

    public EstabelecimentoService(EstabelecimentoRepository estabelecimentos, CapacidadeHospedagemRepository capacidades,
            TipoHospedagemRepository tipos, ControleAcesso acesso, AuditoriaService auditoria) {
        this.estabelecimentos = estabelecimentos;
        this.capacidades = capacidades;
        this.tipos = tipos;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    /** ADMIN e VISUALIZADOR veem todos; PARCEIRO só os seus. Filtros opcionais. */
    @Transactional(readOnly = true)
    public List<EstabelecimentoResumo> listar(UsuarioLogado usuario, String tipo, Boolean ativo) {
        List<Estabelecimento> lista = usuario.isParceiro()
                ? estabelecimentos.findByIdInOrderByNomeFantasia(acesso.estabelecimentosDo(usuario))
                : estabelecimentos.findAllByOrderByNomeFantasia();
        Map<Long, CapacidadeHospedagem> vigentes = capacidadesVigentes(LocalDate.now());
        return lista.stream()
                .filter(e -> tipo == null || tipo.isBlank() || tipo.equals(e.getTipoHospedagemCodigo()))
                .filter(e -> ativo == null || ativo == e.isAtivo())
                .map(e -> EstabelecimentoResumo.de(e, vigentes.get(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EstabelecimentoResponse buscar(Long id, UsuarioLogado usuario) {
        acesso.exigirLeitura(usuario, id);
        Estabelecimento estabelecimento = carregar(id);
        return EstabelecimentoResponse.de(estabelecimento, capacidadeVigente(id));
    }

    @Transactional
    public EstabelecimentoResponse criar(CriacaoRequest requisicao, UsuarioLogado admin) {
        validarTipo(requisicao.secretaria().tipoHospedagemCodigo());
        String cnpj = requisicao.dados().cnpj();
        if (cnpj != null && estabelecimentos.existsByCnpj(cnpj)) {
            throw new RegraNegocioException("Já existe um estabelecimento com este CNPJ.");
        }
        validarCapacidade(requisicao.capacidade());
        Estabelecimento estabelecimento = new Estabelecimento(requisicao.dados(), requisicao.secretaria());
        estabelecimento.marcarInventarioAtualizado();
        estabelecimentos.save(estabelecimento);
        CapacidadeHospedagem capacidade = capacidades.save(novaCapacidade(estabelecimento, requisicao.capacidade(), admin));
        auditoria.registrar(admin.id(), "estabelecimento", estabelecimento.getId(), "CRIAR", null, requisicao);
        return EstabelecimentoResponse.de(estabelecimento, capacidade);
    }

    /** O PARCEIRO altera só os dados cadastrais; o bloco da Secretaria é ignorado para ele. */
    @Transactional
    public EstabelecimentoResponse atualizar(Long id, AtualizacaoRequest requisicao, UsuarioLogado usuario) {
        acesso.exigirEscrita(usuario, id);
        Estabelecimento estabelecimento = carregar(id);
        String cnpj = requisicao.dados().cnpj();
        if (cnpj != null && estabelecimentos.existsByCnpjAndIdNot(cnpj, id)) {
            throw new RegraNegocioException("Já existe outro estabelecimento com este CNPJ.");
        }
        DadosCadastrais dadosAntes = estabelecimento.getDadosCadastrais();
        DadosSecretaria secretariaAntes = estabelecimento.getDadosSecretaria();
        estabelecimento.atualizar(requisicao.dados());
        if (usuario.isAdmin() && requisicao.secretaria() != null) {
            validarTipo(requisicao.secretaria().tipoHospedagemCodigo());
            estabelecimento.atualizar(requisicao.secretaria());
        }
        estabelecimento.marcarInventarioAtualizado();
        auditoria.registrar(usuario.id(), "estabelecimento", id, "ATUALIZAR",
                Map.of("dados", dadosAntes, "secretaria", secretariaAntes),
                Map.of("dados", estabelecimento.getDadosCadastrais(), "secretaria", estabelecimento.getDadosSecretaria()));
        return EstabelecimentoResponse.de(estabelecimento, capacidadeVigente(id));
    }

    /** Não há exclusão: o estabelecimento inativo sai das pesquisas e dos cálculos, mas o histórico fica. */
    @Transactional
    public void definirAtivo(Long id, boolean ativo, UsuarioLogado admin) {
        carregar(id).definirAtivo(ativo);
        auditoria.registrar(admin.id(), "estabelecimento", id, ativo ? "REATIVAR" : "INATIVAR", null, null);
    }

    @Transactional(readOnly = true)
    public List<CapacidadeResponse> capacidades(Long id, UsuarioLogado usuario) {
        acesso.exigirLeitura(usuario, id);
        carregar(id);
        return capacidades.findByEstabelecimentoIdOrderByVigenteDesdeDesc(id).stream()
                .map(CapacidadeResponse::de).toList();
    }

    /** Mudança de UH ou leitos: entra como nova vigência e não altera os períodos anteriores. */
    @Transactional
    public CapacidadeResponse registrarCapacidade(Long id, CapacidadeRequest requisicao, UsuarioLogado usuario) {
        acesso.exigirEscrita(usuario, id);
        Estabelecimento estabelecimento = carregar(id);
        validarCapacidade(requisicao);
        if (capacidades.existsByEstabelecimentoIdAndVigenteDesde(id, requisicao.vigenteDesde())) {
            throw new RegraNegocioException("Já existe uma capacidade a partir desta data. Use outra data.");
        }
        CapacidadeHospedagem capacidade = capacidades.save(novaCapacidade(estabelecimento, requisicao, usuario));
        estabelecimento.marcarInventarioAtualizado();
        auditoria.registrar(usuario.id(), "capacidade_hospedagem", capacidade.getId(), "CRIAR", null, requisicao);
        return CapacidadeResponse.de(capacidade);
    }

    /**
     * Corrige um lançamento de capacidade digitado errado (equipe do OTO). Resultados já
     * publicados não mudam: eles guardam os números da data da publicação.
     */
    @Transactional
    public CapacidadeResponse corrigirCapacidade(Long id, Long capacidadeId, CapacidadeRequest requisicao,
            UsuarioLogado admin) {
        CapacidadeHospedagem capacidade = carregarCapacidade(id, capacidadeId);
        validarCapacidade(requisicao);
        if (!capacidade.getVigenteDesde().equals(requisicao.vigenteDesde())
                && capacidades.existsByEstabelecimentoIdAndVigenteDesde(id, requisicao.vigenteDesde())) {
            throw new RegraNegocioException("Já existe uma capacidade a partir desta data. Use outra data.");
        }
        CapacidadeResponse antes = CapacidadeResponse.de(capacidade);
        capacidade.corrigir(requisicao.vigenteDesde(), requisicao.qtdUh(), requisicao.qtdUhPcd(), requisicao.qtdLeitos(),
                requisicao.maxLeitosPorUh(), admin.id());
        carregar(id).marcarInventarioAtualizado();
        CapacidadeResponse depois = CapacidadeResponse.de(capacidade);
        auditoria.registrar(admin.id(), "capacidade_hospedagem", capacidadeId, "CORRIGIR", antes, depois);
        return depois;
    }

    /** Exclui um lançamento de capacidade feito por engano. A única capacidade não pode ser excluída. */
    @Transactional
    public void excluirCapacidade(Long id, Long capacidadeId, UsuarioLogado admin) {
        CapacidadeHospedagem capacidade = carregarCapacidade(id, capacidadeId);
        if (capacidades.findByEstabelecimentoIdOrderByVigenteDesdeDesc(id).size() == 1) {
            throw new RegraNegocioException(
                    "É a única capacidade do estabelecimento: corrija os números em vez de excluir.");
        }
        CapacidadeResponse antes = CapacidadeResponse.de(capacidade);
        capacidades.delete(capacidade);
        auditoria.registrar(admin.id(), "capacidade_hospedagem", capacidadeId, "EXCLUIR", antes, null);
    }

    private CapacidadeHospedagem carregarCapacidade(Long id, Long capacidadeId) {
        CapacidadeHospedagem capacidade = capacidades.findById(capacidadeId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Capacidade", capacidadeId));
        if (!capacidade.getEstabelecimento().getId().equals(id)) {
            throw RecursoNaoEncontradoException.de("Capacidade", capacidadeId);
        }
        return capacidade;
    }

    /** Capacidade vigente de cada estabelecimento na data, por id do estabelecimento. */
    @Transactional(readOnly = true)
    public Map<Long, CapacidadeHospedagem> capacidadesVigentes(LocalDate data) {
        return capacidades.findVigentesEm(data).stream()
                .collect(Collectors.toMap(c -> c.getEstabelecimento().getId(), Function.identity(), (a, b) -> a));
    }

    private CapacidadeHospedagem capacidadeVigente(Long id) {
        return capacidades.findFirstByEstabelecimentoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                id, LocalDate.now()).orElse(null);
    }

    private Estabelecimento carregar(Long id) {
        return estabelecimentos.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Estabelecimento", id));
    }

    private void validarTipo(String codigo) {
        if (!tipos.existsById(codigo)) {
            throw new RegraNegocioException("Tipo de hospedagem inválido: " + codigo + ". Veja /api/referencias.");
        }
    }

    private static void validarCapacidade(CapacidadeRequest c) {
        if (c.qtdUhPcd() > c.qtdUh()) {
            throw new RegraNegocioException("A quantidade de UH PCD não pode passar o total de UH.");
        }
    }

    private static CapacidadeHospedagem novaCapacidade(Estabelecimento e, CapacidadeRequest c, UsuarioLogado usuario) {
        return new CapacidadeHospedagem(e, c.vigenteDesde(), c.qtdUh(), c.qtdUhPcd(), c.qtdLeitos(),
                c.maxLeitosPorUh(), usuario.id());
    }
}
