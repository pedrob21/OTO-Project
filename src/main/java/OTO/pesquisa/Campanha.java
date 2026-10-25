package OTO.pesquisa;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Período medido dentro de uma rodada (mês ou feriado), consolidado ou expectativa. */
@Entity
@Table(name = "campanha")
public class Campanha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rodada_id")
    private RodadaPesquisa rodada;

    private String nome;

    @Enumerated(EnumType.STRING)
    private Modalidade modalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_periodo")
    private TipoPeriodo tipoPeriodo;

    @Column(name = "feriado_codigo")
    private String feriadoCodigo;

    @Column(name = "periodo_inicio")
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim")
    private LocalDate periodoFim;

    @Column(name = "qtd_diarias")
    private Short qtdDiarias;

    @Column(name = "publicada_em")
    private Instant publicadaEm;

    @Column(name = "publicada_por")
    private Long publicadaPor;

    protected Campanha() {
    }

    public Campanha(RodadaPesquisa rodada, DadosCampanha dados) {
        this.rodada = rodada;
        alterar(dados);
    }

    public void alterar(DadosCampanha dados) {
        this.nome = dados.nome();
        this.modalidade = dados.modalidade();
        this.tipoPeriodo = dados.tipoPeriodo();
        this.feriadoCodigo = dados.tipoPeriodo() == TipoPeriodo.FERIADO ? dados.feriadoCodigo() : null;
        this.periodoInicio = dados.periodoInicio();
        this.periodoFim = dados.periodoFim();
        this.qtdDiarias = dados.qtdDiarias().shortValue();
    }

    public void publicar(Long usuarioId) {
        this.publicadaEm = Instant.now();
        this.publicadaPor = usuarioId;
    }

    public void cancelarPublicacao() {
        this.publicadaEm = null;
        this.publicadaPor = null;
    }

    public boolean isPublicada() {
        return publicadaEm != null;
    }

    public Long getId() { return id; }
    public RodadaPesquisa getRodada() { return rodada; }
    public String getNome() { return nome; }
    public Modalidade getModalidade() { return modalidade; }
    public TipoPeriodo getTipoPeriodo() { return tipoPeriodo; }
    public String getFeriadoCodigo() { return feriadoCodigo; }
    public LocalDate getPeriodoInicio() { return periodoInicio; }
    public LocalDate getPeriodoFim() { return periodoFim; }
    public int getQtdDiarias() { return qtdDiarias; }
    public Instant getPublicadaEm() { return publicadaEm; }
    public Long getPublicadaPor() { return publicadaPor; }
}
