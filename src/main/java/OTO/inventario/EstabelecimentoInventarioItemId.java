package OTO.inventario;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EstabelecimentoInventarioItemId implements Serializable {

    @Column(name = "estabelecimento_id")
    private Long estabelecimentoId;

    @Column(name = "item_id")
    private Long itemId;

    protected EstabelecimentoInventarioItemId() {
    }

    public EstabelecimentoInventarioItemId(Long estabelecimentoId, Long itemId) {
        this.estabelecimentoId = estabelecimentoId;
        this.itemId = itemId;
    }

    public Long getEstabelecimentoId() { return estabelecimentoId; }
    public Long getItemId() { return itemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EstabelecimentoInventarioItemId outro)) {
            return false;
        }
        return Objects.equals(estabelecimentoId, outro.estabelecimentoId) && Objects.equals(itemId, outro.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(estabelecimentoId, itemId);
    }
}
