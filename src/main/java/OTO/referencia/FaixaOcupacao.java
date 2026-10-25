package OTO.referencia;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Faixa de classificação da taxa de ocupação (Ruim a Excelente). */
@Entity
@Table(name = "faixa_ocupacao")
public class FaixaOcupacao {

    @Id
    private String codigo;

    private String nome;

    @Column(name = "percentual_minimo")
    private BigDecimal percentualMinimo;

    protected FaixaOcupacao() {
    }

    public FaixaOcupacao(String codigo, String nome, BigDecimal percentualMinimo) {
        this.codigo = codigo;
        alterar(nome, percentualMinimo);
    }

    /** O código não muda: ele é a chave usada nos resultados publicados. */
    public void alterar(String nome, BigDecimal percentualMinimo) {
        this.nome = nome;
        this.percentualMinimo = percentualMinimo;
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public BigDecimal getPercentualMinimo() { return percentualMinimo; }
}
