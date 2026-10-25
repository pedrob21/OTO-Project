package OTO.inventario;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Catálogo do inventário e itens marcados por estabelecimento. */
public final class InventarioDtos {

    private InventarioDtos() {
    }

    /** Grupo do catálogo com os itens, na ordem da planilha. */
    public record CatalogoGrupo(
            Long id,
            CategoriaInventario categoria,
            String codigo,
            String nome,
            boolean exclusivoSecretaria,
            int ordem,
            boolean ativo,
            List<CatalogoItem> itens) {

        public static CatalogoGrupo de(InventarioGrupo g, List<CatalogoItem> itens) {
            return new CatalogoGrupo(g.getId(), g.getCategoria(), g.getCodigo(), g.getNome(), g.isExclusivoSecretaria(),
                    g.getOrdem(), g.isAtivo(), itens);
        }
    }

    public record CatalogoItem(Long id, String codigo, String nome, String descricao, boolean permiteComplemento,
            int ordem, boolean ativo) {

        public static CatalogoItem de(InventarioItem i) {
            return new CatalogoItem(i.getId(), i.getCodigo(), i.getNome(), i.getDescricao(), i.isPermiteComplemento(),
                    i.getOrdem(), i.isAtivo());
        }
    }

    /**
     * Grupo do catálogo (equipe do OTO). Sem código, ele é gerado a partir do nome;
     * sem ordem, o grupo novo vai para o fim e o alterado fica onde está.
     */
    public record GrupoRequest(
            @NotNull CategoriaInventario categoria,
            @Size(max = 60) String codigo,
            @NotBlank @Size(max = 150) String nome,
            boolean exclusivoSecretaria,
            @Min(1) @Max(9999) Integer ordem) {
    }

    /** Item de um grupo (equipe do OTO). Mesmas regras de código e ordem do grupo. */
    public record ItemRequest(
            @Size(max = 80) String codigo,
            @NotBlank @Size(max = 200) String nome,
            @Size(max = 300) String descricao,
            boolean permiteComplemento,
            @Min(1) @Max(9999) Integer ordem) {
    }

    /** Item que o estabelecimento possui. O complemento só vale para itens como "Outros". */
    public record ItemMarcado(@NotNull Long itemId, @Size(max = 300) String complemento) {
    }

    /**
     * Lista completa dos itens que o estabelecimento possui: o que não vier na lista é
     * desmarcado. Para o PARCEIRO, os grupos exclusivos da Secretaria ficam como estão.
     */
    public record InventarioRequest(@NotNull @Valid List<@NotNull ItemMarcado> itens) {
    }

    public record InventarioResponse(Long estabelecimentoId, Instant inventarioAtualizadoEm, List<ItemMarcado> itens) {
    }
}
