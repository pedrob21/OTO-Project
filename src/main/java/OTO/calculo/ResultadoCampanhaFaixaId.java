package OTO.calculo;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ResultadoCampanhaFaixaId implements Serializable {

    @Column(name = "campanha_id")
    private Long campanhaId;

    @Column(name = "faixa_codigo")
    private String faixaCodigo;

    protected ResultadoCampanhaFaixaId() {
    }

    public ResultadoCampanhaFaixaId(Long campanhaId, String faixaCodigo) {
        this.campanhaId = campanhaId;
        this.faixaCodigo = faixaCodigo;
    }

    public Long getCampanhaId() { return campanhaId; }
    public String getFaixaCodigo() { return faixaCodigo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResultadoCampanhaFaixaId outro)) {
            return false;
        }
        return Objects.equals(campanhaId, outro.campanhaId) && Objects.equals(faixaCodigo, outro.faixaCodigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(campanhaId, faixaCodigo);
    }
}
