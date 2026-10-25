package OTO.inventario;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.ControleAcesso;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.CatalogoItem;
import OTO.inventario.InventarioDtos.InventarioRequest;
import OTO.inventario.InventarioDtos.InventarioResponse;
import OTO.inventario.InventarioDtos.ItemMarcado;

@Service
public class InventarioService {

    private final InventarioItemRepository itens;
    private final EstabelecimentoInventarioItemRepository marcados;
    private final EstabelecimentoRepository estabelecimentos;
    private final ControleAcesso acesso;
    private final AuditoriaService auditoria;

    public InventarioService(InventarioItemRepository itens, EstabelecimentoInventarioItemRepository marcados,
            EstabelecimentoRepository estabelecimentos, ControleAcesso acesso, AuditoriaService auditoria) {
        this.itens = itens;
        this.marcados = marcados;
        this.estabelecimentos = estabelecimentos;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<CatalogoGrupo> catalogo() {
        Map<Long, CatalogoGrupo> grupos = new LinkedHashMap<>();
        for (InventarioItem item : itens.findCatalogoAtivo()) {
            InventarioGrupo g = item.getGrupo();
            grupos.computeIfAbsent(g.getId(), id -> CatalogoGrupo.de(g, new ArrayList<>()))
                    .itens()
                    .add(CatalogoItem.de(item));
        }
        return List.copyOf(grupos.values());
    }

    @Transactional(readOnly = true)
    public InventarioResponse buscar(Long estabelecimentoId, UsuarioLogado usuario) {
        acesso.exigirLeitura(usuario, estabelecimentoId);
        Estabelecimento estabelecimento = carregar(estabelecimentoId);
        return resposta(estabelecimento, marcados.findByIdEstabelecimentoId(estabelecimentoId));
    }

    /**
     * Substitui os itens marcados pelos da lista. Itens inativos do catálogo não são
     * mexidos; para o PARCEIRO, os grupos exclusivos da Secretaria também não.
     */
    @Transactional
    public InventarioResponse salvar(Long estabelecimentoId, InventarioRequest requisicao, UsuarioLogado usuario) {
        acesso.exigirEscrita(usuario, estabelecimentoId);
        Estabelecimento estabelecimento = carregar(estabelecimentoId);

        Map<Long, InventarioItem> catalogo = new HashMap<>();
        itens.findCatalogoAtivo().forEach(item -> catalogo.put(item.getId(), item));

        Map<Long, String> pedidos = new LinkedHashMap<>();
        for (ItemMarcado marcado : requisicao.itens()) {
            InventarioItem item = catalogo.get(marcado.itemId());
            if (item == null) {
                throw new RegraNegocioException("Item de inventário inexistente ou inativo: " + marcado.itemId() + ".");
            }
            if (pedidos.containsKey(item.getId())) {
                throw new RegraNegocioException("O item \"" + item.getNome() + "\" foi enviado mais de uma vez.");
            }
            String complemento = normalizar(marcado.complemento());
            if (complemento != null && !item.isPermiteComplemento()) {
                throw new RegraNegocioException("O item \"" + item.getNome() + "\" não aceita complemento.");
            }
            pedidos.put(item.getId(), complemento);
        }

        List<EstabelecimentoInventarioItem> atuais = marcados.findByIdEstabelecimentoId(estabelecimentoId);
        List<ItemMarcado> antes = paraDto(atuais);
        Map<Long, EstabelecimentoInventarioItem> atuaisPorItem = new HashMap<>();
        for (EstabelecimentoInventarioItem atual : atuais) {
            atuaisPorItem.put(atual.getItemId(), atual);
            InventarioItem item = catalogo.get(atual.getItemId());
            if (item == null || !podeAlterar(item, usuario)) {
                continue;
            }
            if (!pedidos.containsKey(atual.getItemId())) {
                marcados.delete(atual);
            }
            else if (!Objects.equals(atual.getComplemento(), pedidos.get(atual.getItemId()))) {
                atual.alterarComplemento(pedidos.get(atual.getItemId()), usuario.id());
            }
        }
        for (Map.Entry<Long, String> pedido : pedidos.entrySet()) {
            InventarioItem item = catalogo.get(pedido.getKey());
            if (podeAlterar(item, usuario) && !atuaisPorItem.containsKey(pedido.getKey())) {
                marcados.save(new EstabelecimentoInventarioItem(estabelecimentoId, pedido.getKey(), pedido.getValue(),
                        usuario.id()));
            }
        }
        estabelecimento.marcarInventarioAtualizado();

        List<EstabelecimentoInventarioItem> depois = marcados.findByIdEstabelecimentoId(estabelecimentoId);
        auditoria.registrar(usuario.id(), "estabelecimento_inventario", estabelecimentoId, "ATUALIZAR",
                Map.of("itens", antes), Map.of("itens", paraDto(depois)));
        return resposta(estabelecimento, depois);
    }

    private static boolean podeAlterar(InventarioItem item, UsuarioLogado usuario) {
        return usuario.isAdmin() || !item.getGrupo().isExclusivoSecretaria();
    }

    private Estabelecimento carregar(Long id) {
        return estabelecimentos.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Estabelecimento", id));
    }

    private static InventarioResponse resposta(Estabelecimento estabelecimento, List<EstabelecimentoInventarioItem> lista) {
        return new InventarioResponse(estabelecimento.getId(), estabelecimento.getInventarioAtualizadoEm(), paraDto(lista));
    }

    private static List<ItemMarcado> paraDto(List<EstabelecimentoInventarioItem> lista) {
        return lista.stream()
                .sorted(Comparator.comparing(EstabelecimentoInventarioItem::getItemId))
                .map(m -> new ItemMarcado(m.getItemId(), m.getComplemento()))
                .toList();
    }

    private static String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
