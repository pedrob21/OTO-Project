package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cópia de cada envio de uma resposta. Somente inserção: correções não apagam nada. */
@Entity
@Table(name = "resposta_ocupacao_historico")
public class RespostaOcupacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resposta_id", updatable = false)
    private Long respostaId;

    @Column(updatable = false)
    private Integer versao;

    @Column(name = "taxa_ocupacao", updatable = false)
    private BigDecimal taxaOcupacao;

    @Column(name = "diaria_media", updatable = false)
    private BigDecimal diariaMedia;

    @Column(name = "enviado_por", updatable = false)
    private Long enviadoPor;

    @Column(name = "enviado_em", updatable = false)
    private Instant enviadoEm;

    protected RespostaOcupacaoHistorico() {
    }

    public static RespostaOcupacaoHistorico de(RespostaOcupacao resposta) {
        RespostaOcupacaoHistorico h = new RespostaOcupacaoHistorico();
        h.respostaId = resposta.getId();
        h.versao = resposta.getVersao();
        h.taxaOcupacao = resposta.getTaxaOcupacao();
        h.diariaMedia = resposta.getDiariaMedia();
        h.enviadoPor = resposta.getEnviadoPor();
        h.enviadoEm = resposta.getEnviadoEm();
        return h;
    }

    public Long getId() { return id; }
    public Long getRespostaId() { return respostaId; }
    public int getVersao() { return versao; }
    public BigDecimal getTaxaOcupacao() { return taxaOcupacao; }
    public BigDecimal getDiariaMedia() { return diariaMedia; }
    public Long getEnviadoPor() { return enviadoPor; }
    public Instant getEnviadoEm() { return enviadoEm; }
}
