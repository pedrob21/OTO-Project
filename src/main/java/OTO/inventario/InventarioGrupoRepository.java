package OTO.inventario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventarioGrupoRepository extends JpaRepository<InventarioGrupo, Long> {

    boolean existsBySegmentoAndCodigo(String segmento, String codigo);

    boolean existsBySegmentoAndCodigoAndIdNot(String segmento, String codigo, Long id);
}
