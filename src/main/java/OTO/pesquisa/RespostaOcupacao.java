package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;

import OTO.estabelecimento.Estabelecimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Resposta vigente de um meio de hospedagem em uma campanha (vale o último envio). */
@Entity
@Table(name = "resposta_ocupacao")
public class RespostaOcupacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campanha_id")
    private Campanha campanha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id")
    private Estabelecimento estabelecimento;

    @Column(name = "taxa_ocupacao")
    private BigDecimal taxaOcupacao;

    @Column(name = "diaria_media")
    private BigDecimal diariaMedia;

    private Integer versao;

    @Column(name = "enviado_por")
    private Long enviadoPor;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    protected RespostaOcupacao() {
    }

    public RespostaOcupacao(Campanha campanha, Estabelecimento estabelecimento, BigDecimal taxaOcupacao,
            BigDecimal diariaMedia, Long enviadoPor) {
        this.campanha = campanha;
        this.estabelecimento = estabelecimento;
        this.taxaOcupacao = taxaOcupacao;
        this.diariaMedia = diariaMedia;
        this.versao = 1;
        this.enviadoPor = enviadoPor;
        this.enviadoEm = Instant.now();
    }

    /** Novo envio do mesmo estabelecimento: substitui os valores e soma uma versão. */
    public void corrigir(BigDecimal taxaOcupacao, BigDecimal diariaMedia, Long enviadoPor) {
        this.taxaOcupacao = taxaOcupacao;
        this.diariaMedia = diariaMedia;
        this.versao = this.versao + 1;
        this.enviadoPor = enviadoPor;
        this.enviadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Campanha getCampanha() { return campanha; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public BigDecimal getTaxaOcupacao() { return taxaOcupacao; }
    public BigDecimal getDiariaMedia() { return diariaMedia; }
    public int getVersao() { return versao; }
    public Long getEnviadoPor() { return enviadoPor; }
    public Instant getEnviadoEm() { return enviadoEm; }
}
