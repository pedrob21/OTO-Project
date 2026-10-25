package OTO.inventario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventarioItemRepository extends JpaRepository<InventarioItem, Long> {

    /** Catálogo ativo, já com os grupos, na ordem da planilha. */
    @Query("""
            select i from InventarioItem i
            join fetch i.grupo g
            where g.ativo = true and i.ativo = true
            order by g.ordem, i.ordem
            """)
    List<InventarioItem> findCatalogoAtivo();

    List<InventarioItem> findByGrupoIdOrderByOrdemAscIdAsc(Long grupoId);

    boolean existsByGrupoId(Long grupoId);

    boolean existsByGrupoIdAndCodigo(Long grupoId, String codigo);

    boolean existsByGrupoIdAndCodigoAndIdNot(Long grupoId, String codigo, Long id);
}
