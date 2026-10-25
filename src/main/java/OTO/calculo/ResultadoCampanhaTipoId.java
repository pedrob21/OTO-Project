package OTO.calculo;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ResultadoCampanhaTipoId implements Serializable {

    @Column(name = "campanha_id")
    private Long campanhaId;

    @Column(name = "tipo_hospedagem_codigo")
    private String tipoHospedagemCodigo;

    protected ResultadoCampanhaTipoId() {
    }

    public ResultadoCampanhaTipoId(Long campanhaId, String tipoHospedagemCodigo) {
        this.campanhaId = campanhaId;
        this.tipoHospedagemCodigo = tipoHospedagemCodigo;
    }

    public Long getCampanhaId() { return campanhaId; }
    public String getTipoHospedagemCodigo() { return tipoHospedagemCodigo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResultadoCampanhaTipoId outro)) {
            return false;
        }
        return Objects.equals(campanhaId, outro.campanhaId)
                && Objects.equals(tipoHospedagemCodigo, outro.tipoHospedagemCodigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(campanhaId, tipoHospedagemCodigo);
    }
}
