package OTO.calculo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** entidade que mapeia o resultado de uma campanha publicada por tipo de hospedagem. */
@Entity
@Table(name = "resultado_campanha_tipo")
public class ResultadoCampanhaTipo {

    @EmbeddedId
    private ResultadoCampanhaTipoId id;

    @Column(name = "qtd_mh")
    private Integer qtdMh;

    @Column(name = "qtd_mh_respondentes")
    private Integer qtdMhRespondentes;

    @Column(name = "qtd_uh")
    private Integer qtdUh;

    @Column(name = "taxa_ocupacao")
    private BigDecimal taxaOcupacao;

    @Column(name = "diaria_media")
    private BigDecimal diariaMedia;

    private BigDecimal revpar;

    @Column(name = "receita_estimada")
    private BigDecimal receitaEstimada;

    protected ResultadoCampanhaTipo() {
    }

    public static ResultadoCampanhaTipo de(Long campanhaId, CalculadoraOcupacao.ResultadoTipo r) {
        ResultadoCampanhaTipo e = new ResultadoCampanhaTipo();
        e.id = new ResultadoCampanhaTipoId(campanhaId, r.tipo());
        e.qtdMh = r.qtdMh();
        e.qtdMhRespondentes = r.qtdMhRespondentes();
        e.qtdUh = r.qtdUh();
        e.taxaOcupacao = Numeros.duasCasas(r.taxaOcupacao());
        e.diariaMedia = Numeros.duasCasas(r.diariaMedia());
        e.revpar = Numeros.duasCasas(r.revpar());
        e.receitaEstimada = Numeros.duasCasas(r.receitaEstimada());
        return e;
    }

    public String getTipoHospedagemCodigo() { return id.getTipoHospedagemCodigo(); }
    public int getQtdMh() { return qtdMh; }
    public int getQtdMhRespondentes() { return qtdMhRespondentes; }
    public int getQtdUh() { return qtdUh; }
    public BigDecimal getTaxaOcupacao() { return taxaOcupacao; }
    public BigDecimal getDiariaMedia() { return diariaMedia; }
    public BigDecimal getRevpar() { return revpar; }
    public BigDecimal getReceitaEstimada() { return receitaEstimada; }
}
