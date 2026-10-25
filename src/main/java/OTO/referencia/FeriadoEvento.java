package OTO.referencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Feriado prolongado ou evento com pesquisa própria (tabela de referência mantida pela equipe do OTO). */
@Entity
@Table(name = "feriado_evento")
public class FeriadoEvento {

    @Id
    private String codigo;

    private String nome;

    private Short ordem;

    protected FeriadoEvento() {
    }

    public FeriadoEvento(String codigo, String nome, int ordem) {
        this.codigo = codigo;
        alterar(nome, ordem);
    }

    /** O código não muda: ele é a chave usada nas campanhas e na série histórica. */
    public void alterar(String nome, int ordem) {
        this.nome = nome;
        this.ordem = (short) ordem;
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public Short getOrdem() { return ordem; }
}
