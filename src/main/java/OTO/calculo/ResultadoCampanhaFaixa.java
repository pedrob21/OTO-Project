package OTO.calculo;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** entidade que mapeia quantos meios de hospedagem respondentes ficaram em uma faixa de ocupação. */
@Entity
@Table(name = "resultado_campanha_faixa")
public class ResultadoCampanhaFaixa {

    @EmbeddedId
    private ResultadoCampanhaFaixaId id;

    @Column(name = "qtd_mh")
    private Integer qtdMh;

    protected ResultadoCampanhaFaixa() {
    }

    public ResultadoCampanhaFaixa(Long campanhaId, String faixaCodigo, int qtdMh) {
        this.id = new ResultadoCampanhaFaixaId(campanhaId, faixaCodigo);
        this.qtdMh = qtdMh;
    }

    public String getFaixaCodigo() { return id.getFaixaCodigo(); }
    public int getQtdMh() { return qtdMh; }
}
