package OTO.estabelecimento;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Totais de hospedagem caseira (RHC) a partir de uma data. */
@Entity
@Table(name = "rhc_resumo")
public class RhcResumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "qtd_registros")
    private Integer qtdRegistros;

    @Column(name = "qtd_uh")
    private Integer qtdUh;

    @Column(name = "qtd_leitos")
    private Integer qtdLeitos;

    @Column(name = "max_leitos_por_uh")
    private Short maxLeitosPorUh;

    @Column(name = "registrado_por")
    private Long registradoPor;

    @Column(name = "registrado_em")
    private Instant registradoEm;

    protected RhcResumo() {
    }

    public RhcResumo(LocalDate vigenteDesde, int qtdRegistros, int qtdUh, int qtdLeitos, int maxLeitosPorUh,
            Long registradoPor) {
        this.vigenteDesde = vigenteDesde;
        this.qtdRegistros = qtdRegistros;
        this.qtdUh = qtdUh;
        this.qtdLeitos = qtdLeitos;
        this.maxLeitosPorUh = (short) maxLeitosPorUh;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    /** Correção de um lançamento digitado errado (feita pela equipe do OTO). */
    public void corrigir(LocalDate vigenteDesde, int qtdRegistros, int qtdUh, int qtdLeitos, int maxLeitosPorUh,
            Long registradoPor) {
        this.vigenteDesde = vigenteDesde;
        this.qtdRegistros = qtdRegistros;
        this.qtdUh = qtdUh;
        this.qtdLeitos = qtdLeitos;
        this.maxLeitosPorUh = (short) maxLeitosPorUh;
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    public double getLeitosUteis() {
        return qtdUh * (1 + maxLeitosPorUh) / 2.0;
    }

    public Long getId() { return id; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public int getQtdRegistros() { return qtdRegistros; }
    public int getQtdUh() { return qtdUh; }
    public int getQtdLeitos() { return qtdLeitos; }
    public int getMaxLeitosPorUh() { return maxLeitosPorUh; }
    public Instant getRegistradoEm() { return registradoEm; }
}
