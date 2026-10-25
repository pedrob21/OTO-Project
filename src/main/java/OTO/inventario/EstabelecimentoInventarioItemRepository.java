package OTO.inventario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstabelecimentoInventarioItemRepository
        extends JpaRepository<EstabelecimentoInventarioItem, EstabelecimentoInventarioItemId> {

    List<EstabelecimentoInventarioItem> findByIdEstabelecimentoId(Long estabelecimentoId);

    /** Algum estabelecimento marcou o item? */
    boolean existsByIdItemId(Long itemId);

    /** Algum estabelecimento escreveu complemento (texto livre) no item? */
    boolean existsByIdItemIdAndComplementoIsNotNull(Long itemId);
}
