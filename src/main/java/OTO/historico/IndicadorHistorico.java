package OTO.historico;

import java.math.BigDecimal;
import java.time.Instant;

import OTO.pesquisa.Modalidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Valor da série histórica anterior ao sistema (de um mês ou de um feriado). */
@Entity
@Table(name = "indicador_historico")
public class IndicadorHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Indicador indicador;

    @Enumerated(EnumType.STRING)
    private Modalidade modalidade;

    private Short ano;

    private Short mes;

    @Column(name = "feriado_codigo")
    private String feriadoCodigo;

    @Column(name = "tipo_hospedagem_codigo")
    private String tipoHospedagemCodigo;

    private BigDecimal valor;

    private String observacao;

    private String fonte;

    @Column(name = "importado_em")
    private Instant importadoEm;

    protected IndicadorHistorico() {
    }

    public IndicadorHistorico(Indicador indicador, Modalidade modalidade, int ano, Integer mes, String feriadoCodigo,
            String tipoHospedagemCodigo) {
        corrigirCombinacao(indicador, modalidade, ano, mes, feriadoCodigo, tipoHospedagemCodigo);
    }

    /** Corrige o indicador, o período ou o tipo de um valor lançado errado. */
    public void corrigirCombinacao(Indicador indicador, Modalidade modalidade, int ano, Integer mes, String feriadoCodigo,
            String tipoHospedagemCodigo) {
        this.indicador = indicador;
        this.modalidade = modalidade;
        this.ano = (short) ano;
        this.mes = mes == null ? null : mes.shortValue();
        this.feriadoCodigo = feriadoCodigo;
        this.tipoHospedagemCodigo = tipoHospedagemCodigo;
    }

    public void definirValor(BigDecimal valor, String observacao, String fonte) {
        this.valor = valor;
        this.observacao = observacao;
        this.fonte = fonte;
        this.importadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Indicador getIndicador() { return indicador; }
    public Modalidade getModalidade() { return modalidade; }
    public int getAno() { return ano; }
    public Integer getMes() { return mes == null ? null : mes.intValue(); }
    public String getFeriadoCodigo() { return feriadoCodigo; }
    public String getTipoHospedagemCodigo() { return tipoHospedagemCodigo; }
    public BigDecimal getValor() { return valor; }
    public String getObservacao() { return observacao; }
    public String getFonte() { return fonte; }
}
