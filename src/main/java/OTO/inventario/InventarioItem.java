package OTO.inventario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Opção de um grupo do inventário (ex.: Piscina, Sauna). */
@Entity
@Table(name = "inventario_item")
public class InventarioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id")
    private InventarioGrupo grupo;

    private String codigo;

    private String nome;

    private String descricao;

    @Column(name = "permite_complemento")
    private boolean permiteComplemento;

    private Short ordem;

    private boolean ativo;

    protected InventarioItem() {
    }

    public InventarioItem(InventarioGrupo grupo, String codigo, String nome, String descricao,
            boolean permiteComplemento, int ordem) {
        this.grupo = grupo;
        this.ativo = true;
        alterar(codigo, nome, descricao, permiteComplemento, ordem);
    }

    public void alterar(String codigo, String nome, String descricao, boolean permiteComplemento, int ordem) {
        this.codigo = codigo;
        this.nome = nome;
        this.descricao = descricao;
        this.permiteComplemento = permiteComplemento;
        this.ordem = (short) ordem;
    }

    /** Item inativo sai do formulário; as marcações dos estabelecimentos ficam guardadas. */
    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public InventarioGrupo getGrupo() { return grupo; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public boolean isPermiteComplemento() { return permiteComplemento; }
    public Short getOrdem() { return ordem; }
    public boolean isAtivo() { return ativo; }
}
