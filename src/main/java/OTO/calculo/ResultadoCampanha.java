package OTO.calculo;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * entidade de resultado global de uma campanha, gravado na publicação e nunca mais alterado.
 * {@link #de} também serve para a prévia: o objeto só vai para o banco se for salvo.
 */
@Entity
@Table(name = "resultado_campanha")
public class ResultadoCampanha {

    @Id
    @Column(name = "campanha_id")
    private Long campanhaId;

    @Column(name = "parametro_calculo_id")
    private Long parametroCalculoId;

    @Column(name = "taxa_ocupacao")
    private BigDecimal taxaOcupacao;

    @Column(name = "qtd_mh")
    private Integer qtdMh;

    @Column(name = "qtd_mh_respondentes")
    private Integer qtdMhRespondentes;

    @Column(name = "leitos_total")
    private Integer leitosTotal;

    @Column(name = "leitos_respondentes")
    private Integer leitosRespondentes;

    @Column(name = "percentual_leitos_respondentes")
    private BigDecimal percentualLeitosRespondentes;

    @Column(name = "fluxo_mh")
    private BigDecimal fluxoMh;

    @Column(name = "fluxo_rhc")
    private BigDecimal fluxoRhc;

    @Column(name = "fluxo_day_use")
    private BigDecimal fluxoDayUse;

    @Column(name = "fluxo_total")
    private BigDecimal fluxoTotal;

    @Column(name = "fluxo_medio_diario")
    private BigDecimal fluxoMedioDiario;

    @Column(name = "diaria_media")
    private BigDecimal diariaMedia;

    @Column(name = "receita_hospedagem")
    private BigDecimal receitaHospedagem;

    @Column(name = "receita_sem_hospedagem")
    private BigDecimal receitaSemHospedagem;

    @Column(name = "calculado_em")
    private Instant calculadoEm;

    protected ResultadoCampanha() {
    }

    public static ResultadoCampanha de(Long campanhaId, Long parametroCalculoId, CalculadoraOcupacao.Resultado r) {
        ResultadoCampanha e = new ResultadoCampanha();
        e.campanhaId = campanhaId;
        e.parametroCalculoId = parametroCalculoId;
        e.taxaOcupacao = Numeros.duasCasas(r.taxaOcupacao());
        e.qtdMh = r.qtdMh();
        e.qtdMhRespondentes = r.qtdMhRespondentes();
        e.leitosTotal = r.leitosTotal();
        e.leitosRespondentes = r.leitosRespondentes();
        e.percentualLeitosRespondentes = Numeros.duasCasas(r.percentualLeitosRespondentes());
        e.fluxoMh = Numeros.duasCasas(r.fluxoMh());
        e.fluxoRhc = Numeros.duasCasas(r.fluxoRhc());
        e.fluxoDayUse = Numeros.duasCasas(r.fluxoDayUse());
        e.fluxoTotal = Numeros.duasCasas(r.fluxoTotal());
        e.fluxoMedioDiario = Numeros.duasCasas(r.fluxoMedioDiario());
        e.diariaMedia = Numeros.duasCasas(r.diariaMedia());
        e.receitaHospedagem = Numeros.duasCasas(r.receitaHospedagem());
        e.receitaSemHospedagem = Numeros.duasCasas(r.receitaSemHospedagem());
        e.calculadoEm = Instant.now();
        return e;
    }

    public Long getCampanhaId() { return campanhaId; }
    public Long getParametroCalculoId() { return parametroCalculoId; }
    public BigDecimal getTaxaOcupacao() { return taxaOcupacao; }
    public int getQtdMh() { return qtdMh; }
    public int getQtdMhRespondentes() { return qtdMhRespondentes; }
    public int getLeitosTotal() { return leitosTotal; }
    public int getLeitosRespondentes() { return leitosRespondentes; }
    public BigDecimal getPercentualLeitosRespondentes() { return percentualLeitosRespondentes; }
    public BigDecimal getFluxoMh() { return fluxoMh; }
    public BigDecimal getFluxoRhc() { return fluxoRhc; }
    public BigDecimal getFluxoDayUse() { return fluxoDayUse; }
    public BigDecimal getFluxoTotal() { return fluxoTotal; }
    public BigDecimal getFluxoMedioDiario() { return fluxoMedioDiario; }
    public BigDecimal getDiariaMedia() { return diariaMedia; }
    public BigDecimal getReceitaHospedagem() { return receitaHospedagem; }
    public BigDecimal getReceitaSemHospedagem() { return receitaSemHospedagem; }
    public Instant getCalculadoEm() { return calculadoEm; }
}
