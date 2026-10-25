package OTO.inventario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.Codigos;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.CatalogoItem;
import OTO.inventario.InventarioDtos.GrupoRequest;
import OTO.inventario.InventarioDtos.ItemRequest;

/**
 * Manutenção do catálogo do inventário pela equipe do OTO: grupos e itens novos,
 * correções, inativação e exclusão do que nunca foi usado.
 */
@Service
public class CatalogoInventarioService {

    private static final int TAMANHO_CODIGO_GRUPO = 60;
    private static final int TAMANHO_CODIGO_ITEM = 80;
    private static final String SEGMENTO = InventarioGrupo.SEGMENTO_MEIO_HOSPEDAGEM;

    private final InventarioGrupoRepository grupos;
    private final InventarioItemRepository itens;
    private final EstabelecimentoInventarioItemRepository marcados;
    private final AuditoriaService auditoria;

    public CatalogoInventarioService(InventarioGrupoRepository grupos, InventarioItemRepository itens,
            EstabelecimentoInventarioItemRepository marcados, AuditoriaService auditoria) {
        this.grupos = grupos;
        this.itens = itens;
        this.marcados = marcados;
        this.auditoria = auditoria;
    }

    /** Catálogo inteiro, com o que está inativo e os grupos ainda sem itens. */
    @Transactional(readOnly = true)
    public List<CatalogoGrupo> completo() {
        // Grupos antes dos itens: assim o grupo de cada item já está carregado (sem uma consulta por grupo).
        List<InventarioGrupo> todos = grupos.findAll(Sort.by("ordem", "id"));
        Map<Long, List<CatalogoItem>> porGrupo = new HashMap<>();
        for (InventarioItem item : itens.findAll(Sort.by("ordem", "id"))) {
            porGrupo.computeIfAbsent(item.getGrupo().getId(), id -> new ArrayList<>()).add(CatalogoItem.de(item));
        }
        return todos.stream()
                .map(g -> CatalogoGrupo.de(g, porGrupo.getOrDefault(g.getId(), List.of())))
                .toList();
    }

    // ------------------------------------------------------------------ grupos

    @Transactional
    public CatalogoGrupo criarGrupo(GrupoRequest r, UsuarioLogado admin) {
        String nome = r.nome().trim();
        String codigo;
        if (Codigos.vazio(r.codigo())) {
            codigo = Codigos.livre(nome, TAMANHO_CODIGO_GRUPO, c -> grupos.existsBySegmentoAndCodigo(SEGMENTO, c));
        }
        else {
            codigo = Codigos.informado(r.codigo(), TAMANHO_CODIGO_GRUPO);
            if (grupos.existsBySegmentoAndCodigo(SEGMENTO, codigo)) {
                throw new RegraNegocioException("Já existe um grupo com o código " + codigo + ".");
            }
        }
        int ordem = r.ordem() != null ? r.ordem() : proxima(grupos.findAll().stream().map(InventarioGrupo::getOrdem));
        InventarioGrupo grupo = grupos.save(new InventarioGrupo(r.categoria(), codigo, nome, r.exclusivoSecretaria(), ordem));
        CatalogoGrupo criado = CatalogoGrupo.de(grupo, List.of());
        auditoria.registrar(admin.id(), "inventario_grupo", grupo.getId(), "CRIAR", null, criado);
        return criado;
    }

    @Transactional
    public CatalogoGrupo alterarGrupo(Long id, GrupoRequest r, UsuarioLogado admin) {
        InventarioGrupo grupo = carregarGrupo(id);
        String codigo = Codigos.vazio(r.codigo()) ? grupo.getCodigo() : Codigos.informado(r.codigo(), TAMANHO_CODIGO_GRUPO);
        if (!codigo.equals(grupo.getCodigo()) && grupos.existsBySegmentoAndCodigoAndIdNot(SEGMENTO, codigo, id)) {
            throw new RegraNegocioException("Já existe um grupo com o código " + codigo + ".");
        }
        CatalogoGrupo antes = CatalogoGrupo.de(grupo, List.of());
        grupo.alterar(r.categoria(), codigo, r.nome().trim(), r.exclusivoSecretaria(),
                r.ordem() != null ? r.ordem() : grupo.getOrdem());
        auditoria.registrar(admin.id(), "inventario_grupo", id, "ATUALIZAR", antes, CatalogoGrupo.de(grupo, List.of()));
        return CatalogoGrupo.de(grupo, itens.findByGrupoIdOrderByOrdemAscIdAsc(id).stream().map(CatalogoItem::de).toList());
    }

    @Transactional
    public void definirAtivoGrupo(Long id, boolean ativo, UsuarioLogado admin) {
        carregarGrupo(id).definirAtivo(ativo);
        auditoria.registrar(admin.id(), "inventario_grupo", id, ativo ? "REATIVAR" : "INATIVAR", null, null);
    }

    /** Só grupo sem itens; o que já tem itens é inativado. */
    @Transactional
    public void excluirGrupo(Long id, UsuarioLogado admin) {
        InventarioGrupo grupo = carregarGrupo(id);
        if (itens.existsByGrupoId(id)) {
            throw new RegraNegocioException("O grupo tem itens. Exclua os itens antes ou inative o grupo.");
        }
        CatalogoGrupo antes = CatalogoGrupo.de(grupo, List.of());
        grupos.delete(grupo);
        auditoria.registrar(admin.id(), "inventario_grupo", id, "EXCLUIR", antes, null);
    }

    // ------------------------------------------------------------------ itens

    @Transactional
    public CatalogoItem criarItem(Long grupoId, ItemRequest r, UsuarioLogado admin) {
        InventarioGrupo grupo = carregarGrupo(grupoId);
        String nome = r.nome().trim();
        String codigo;
        if (Codigos.vazio(r.codigo())) {
            codigo = Codigos.livre(nome, TAMANHO_CODIGO_ITEM, c -> itens.existsByGrupoIdAndCodigo(grupoId, c));
        }
        else {
            codigo = Codigos.informado(r.codigo(), TAMANHO_CODIGO_ITEM);
            if (itens.existsByGrupoIdAndCodigo(grupoId, codigo)) {
                throw new RegraNegocioException("Já existe um item com o código " + codigo + " neste grupo.");
            }
        }
        int ordem = r.ordem() != null ? r.ordem()
                : proxima(itens.findByGrupoIdOrderByOrdemAscIdAsc(grupoId).stream().map(InventarioItem::getOrdem));
        InventarioItem item = itens.save(new InventarioItem(grupo, codigo, nome, texto(r.descricao()),
                r.permiteComplemento(), ordem));
        CatalogoItem criado = CatalogoItem.de(item);
        auditoria.registrar(admin.id(), "inventario_item", item.getId(), "CRIAR", null, criado);
        return criado;
    }

    @Transactional
    public CatalogoItem alterarItem(Long id, ItemRequest r, UsuarioLogado admin) {
        InventarioItem item = carregarItem(id);
        Long grupoId = item.getGrupo().getId();
        String codigo = Codigos.vazio(r.codigo()) ? item.getCodigo() : Codigos.informado(r.codigo(), TAMANHO_CODIGO_ITEM);
        if (!codigo.equals(item.getCodigo()) && itens.existsByGrupoIdAndCodigoAndIdNot(grupoId, codigo, id)) {
            throw new RegraNegocioException("Já existe um item com o código " + codigo + " neste grupo.");
        }
        if (item.isPermiteComplemento() && !r.permiteComplemento() && marcados.existsByIdItemIdAndComplementoIsNotNull(id)) {
            throw new RegraNegocioException("Há estabelecimentos com texto neste item. Para ele deixar de aceitar "
                    + "complemento, crie um item novo e inative este.");
        }
        CatalogoItem antes = CatalogoItem.de(item);
        item.alterar(codigo, r.nome().trim(), texto(r.descricao()), r.permiteComplemento(),
                r.ordem() != null ? r.ordem() : item.getOrdem());
        CatalogoItem depois = CatalogoItem.de(item);
        auditoria.registrar(admin.id(), "inventario_item", id, "ATUALIZAR", antes, depois);
        return depois;
    }

    @Transactional
    public void definirAtivoItem(Long id, boolean ativo, UsuarioLogado admin) {
        carregarItem(id).definirAtivo(ativo);
        auditoria.registrar(admin.id(), "inventario_item", id, ativo ? "REATIVAR" : "INATIVAR", null, null);
    }

    /** Só item que nenhum estabelecimento marcou; o que já foi marcado é inativado. */
    @Transactional
    public void excluirItem(Long id, UsuarioLogado admin) {
        InventarioItem item = carregarItem(id);
        if (marcados.existsByIdItemId(id)) {
            throw new RegraNegocioException("Há estabelecimentos com este item marcado. Inative o item em vez de excluir.");
        }
        CatalogoItem antes = CatalogoItem.de(item);
        itens.delete(item);
        auditoria.registrar(admin.id(), "inventario_item", id, "EXCLUIR", antes, null);
    }

    // ------------------------------------------------------------------ apoio

    private InventarioGrupo carregarGrupo(Long id) {
        return grupos.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Grupo do inventário", id));
    }

    private InventarioItem carregarItem(Long id) {
        return itens.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Item do inventário", id));
    }

    /** Próxima posição depois da última. */
    private static int proxima(Stream<Short> ordens) {
        int maior = ordens.mapToInt(Short::intValue).max().orElse(0);
        return Math.min(maior + 1, Short.MAX_VALUE);
    }

    private static String texto(String valor) {
        return Codigos.vazio(valor) ? null : valor.trim();
    }
}
