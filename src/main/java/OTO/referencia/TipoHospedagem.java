package OTO.referencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tipo de meio de hospedagem (tabela de referência mantida pela equipe do OTO). */
@Entity
@Table(name = "tipo_hospedagem")
public class TipoHospedagem {

    @Id
    private String codigo;

    private String nome;

    @Column(name = "calcula_receita")
    private boolean calculaReceita;

    private Short ordem;

    protected TipoHospedagem() {
    }

    public TipoHospedagem(String codigo, String nome, boolean calculaReceita, int ordem) {
        this.codigo = codigo;
        alterar(nome, calculaReceita, ordem);
    }

    /** O código não muda: ele é a chave usada nos estabelecimentos, resultados e série histórica. */
    public void alterar(String nome, boolean calculaReceita, int ordem) {
        this.nome = nome;
        this.calculaReceita = calculaReceita;
        this.ordem = (short) ordem;
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public boolean isCalculaReceita() { return calculaReceita; }
    public Short getOrdem() { return ordem; }
}
