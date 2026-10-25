package OTO.inventario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Item do inventário que um estabelecimento possui. */
@Entity
@Table(name = "estabelecimento_inventario_item")
public class EstabelecimentoInventarioItem {

    @EmbeddedId
    private EstabelecimentoInventarioItemId id;

    private String complemento;

    @Column(name = "registrado_por")
    private Long registradoPor;

    @Column(name = "registrado_em")
    private Instant registradoEm;

    protected EstabelecimentoInventarioItem() {
    }

    public EstabelecimentoInventarioItem(Long estabelecimentoId, Long itemId, String complemento, Long registradoPor) {
        this.id = new EstabelecimentoInventarioItemId(estabelecimentoId, itemId);
        this.complemento = complemento;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    public void alterarComplemento(String complemento, Long registradoPor) {
        this.complemento = complemento;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    public EstabelecimentoInventarioItemId getId() { return id; }
    public Long getItemId() { return id.getItemId(); }
    public String getComplemento() { return complemento; }
}
