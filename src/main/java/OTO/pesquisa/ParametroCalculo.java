package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Parâmetros da metodologia a partir de uma data. Mudanças entram como nova vigência. */
@Entity
@Table(name = "parametro_calculo")
public class ParametroCalculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "percentual_day_use")
    private BigDecimal percentualDayUse;

    @Column(name = "dias_permanencia_media")
    private Short diasPermanenciaMedia;

    @Column(name = "dias_em_parques")
    private Short diasEmParques;

    @Column(name = "gasto_medio_diario")
    private BigDecimal gastoMedioDiario;

    @Column(name = "gasto_medio_ingresso")
    private BigDecimal gastoMedioIngresso;

    @Column(name = "percentual_minimo_leitos_publicacao")
    private BigDecimal percentualMinimoLeitosPublicacao;

    private String fonte;

    @Column(name = "registrado_por")
    private Long registradoPor;

    @Column(name = "registrado_em")
    private Instant registradoEm;

    protected ParametroCalculo() {
    }

    public ParametroCalculo(LocalDate vigenteDesde, BigDecimal percentualDayUse, int diasPermanenciaMedia,
            int diasEmParques, BigDecimal gastoMedioDiario, BigDecimal gastoMedioIngresso,
            BigDecimal percentualMinimoLeitosPublicacao, String fonte, Long registradoPor) {
        alterar(vigenteDesde, percentualDayUse, diasPermanenciaMedia, diasEmParques, gastoMedioDiario,
                gastoMedioIngresso, percentualMinimoLeitosPublicacao, fonte, registradoPor);
    }

    /** Correção de uma vigência que ainda não foi usada em nenhum resultado publicado. */
    public void alterar(LocalDate vigenteDesde, BigDecimal percentualDayUse, int diasPermanenciaMedia,
            int diasEmParques, BigDecimal gastoMedioDiario, BigDecimal gastoMedioIngresso,
            BigDecimal percentualMinimoLeitosPublicacao, String fonte, Long registradoPor) {
        this.vigenteDesde = vigenteDesde;
        this.percentualDayUse = percentualDayUse;
        this.diasPermanenciaMedia = (short) diasPermanenciaMedia;
        this.diasEmParques = (short) diasEmParques;
        this.gastoMedioDiario = gastoMedioDiario;
        this.gastoMedioIngresso = gastoMedioIngresso;
        this.percentualMinimoLeitosPublicacao = percentualMinimoLeitosPublicacao;
        this.fonte = fonte;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    public Long getId() { return id; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public BigDecimal getPercentualDayUse() { return percentualDayUse; }
    public int getDiasPermanenciaMedia() { return diasPermanenciaMedia; }
    public int getDiasEmParques() { return diasEmParques; }
    public BigDecimal getGastoMedioDiario() { return gastoMedioDiario; }
    public BigDecimal getGastoMedioIngresso() { return gastoMedioIngresso; }
    public BigDecimal getPercentualMinimoLeitosPublicacao() { return percentualMinimoLeitosPublicacao; }
    public String getFonte() { return fonte; }
    public Instant getRegistradoEm() { return registradoEm; }
}
