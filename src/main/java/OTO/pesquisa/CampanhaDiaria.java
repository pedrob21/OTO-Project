package OTO.pesquisa;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Diária média de um tipo (ou a geral, com tipo nulo) digitada pela equipe para uma campanha. */
@Entity
@Table(name = "campanha_diaria")
public class CampanhaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campanha_id")
    private Long campanhaId;

    @Column(name = "tipo_hospedagem_codigo")
    private String tipoHospedagemCodigo;

    @Column(name = "diaria_media")
    private BigDecimal diariaMedia;

    private String fonte;

    @Column(name = "registrado_por")
    private Long registradoPor;

    @Column(name = "registrado_em")
    private Instant registradoEm;

    protected CampanhaDiaria() {
    }

    public CampanhaDiaria(Long campanhaId, String tipoHospedagemCodigo, BigDecimal diariaMedia, String fonte,
            Long registradoPor) {
        this.campanhaId = campanhaId;
        this.tipoHospedagemCodigo = tipoHospedagemCodigo;
        alterar(diariaMedia, fonte, registradoPor);
    }

    public void alterar(BigDecimal diariaMedia, String fonte, Long registradoPor) {
        this.diariaMedia = diariaMedia;
        this.fonte = fonte;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    public boolean isGeral() {
        return tipoHospedagemCodigo == null;
    }

    public Long getId() { return id; }
    public Long getCampanhaId() { return campanhaId; }
    public String getTipoHospedagemCodigo() { return tipoHospedagemCodigo; }
    public BigDecimal getDiariaMedia() { return diariaMedia; }
    public String getFonte() { return fonte; }
    public Long getRegistradoPor() { return registradoPor; }
    public Instant getRegistradoEm() { return registradoEm; }
}
