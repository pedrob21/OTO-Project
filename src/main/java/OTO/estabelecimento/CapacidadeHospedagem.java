package OTO.estabelecimento;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * capacidade de um meio de hospedagem a partir de uma data. Mudança de capacidade entra
 * como nova vigência; {@link #corrigir} é só para lançamento digitado errado.
 */
@Entity
@Table(name = "capacidade_hospedagem")
public class CapacidadeHospedagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id")
    private Estabelecimento estabelecimento;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "qtd_uh")
    private Integer qtdUh;

    @Column(name = "qtd_uh_pcd")
    private Integer qtdUhPcd;

    @Column(name = "qtd_leitos")
    private Integer qtdLeitos;

    @Column(name = "max_leitos_por_uh")
    private Short maxLeitosPorUh;

    @Column(name = "registrado_por")
    private Long registradoPor;

    @Column(name = "registrado_em")
    private Instant registradoEm;

    protected CapacidadeHospedagem() {
    }

    public CapacidadeHospedagem(Estabelecimento estabelecimento, LocalDate vigenteDesde, int qtdUh, int qtdUhPcd,
            int qtdLeitos, int maxLeitosPorUh, Long registradoPor) {
        this.estabelecimento = estabelecimento;
        this.vigenteDesde = vigenteDesde;
        this.qtdUh = qtdUh;
        this.qtdUhPcd = qtdUhPcd;
        this.qtdLeitos = qtdLeitos;
        this.maxLeitosPorUh = (short) maxLeitosPorUh;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    /** Correção de um lançamento digitado errado (feita pela equipe do OTO). */
    public void corrigir(LocalDate vigenteDesde, int qtdUh, int qtdUhPcd, int qtdLeitos, int maxLeitosPorUh,
            Long registradoPor) {
        this.vigenteDesde = vigenteDesde;
        this.qtdUh = qtdUh;
        this.qtdUhPcd = qtdUhPcd;
        this.qtdLeitos = qtdLeitos;
        this.maxLeitosPorUh = (short) maxLeitosPorUh;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    /** Metodologia do OTO: UH x média entre 1 e a capacidade máxima por UH. */
    public double getLeitosUteis() {
        return qtdUh * (1 + maxLeitosPorUh) / 2.0;
    }

    public Long getId() { return id; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public int getQtdUh() { return qtdUh; }
    public int getQtdUhPcd() { return qtdUhPcd; }
    public int getQtdLeitos() { return qtdLeitos; }
    public int getMaxLeitosPorUh() { return maxLeitosPorUh; }
    public Long getRegistradoPor() { return registradoPor; }
    public Instant getRegistradoEm() { return registradoEm; }
}
