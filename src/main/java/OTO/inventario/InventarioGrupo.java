package OTO.inventario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Grupo de perguntas do inventário (ex.: Área de recreação, Rampa, Indicadores ODS). */
@Entity
@Table(name = "inventario_grupo")
public class InventarioGrupo {

    /** Único segmento do inventário por enquanto (a tabela já prevê outros). */
    public static final String SEGMENTO_MEIO_HOSPEDAGEM = "MEIO_HOSPEDAGEM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String segmento;

    @Enumerated(EnumType.STRING)
    private CategoriaInventario categoria;

    private String codigo;

    private String nome;

    @Column(name = "exclusivo_secretaria")
    private boolean exclusivoSecretaria;

    private Short ordem;

    private boolean ativo;

    protected InventarioGrupo() {
    }

    public InventarioGrupo(CategoriaInventario categoria, String codigo, String nome, boolean exclusivoSecretaria,
            int ordem) {
        this.segmento = SEGMENTO_MEIO_HOSPEDAGEM;
        this.ativo = true;
        alterar(categoria, codigo, nome, exclusivoSecretaria, ordem);
    }

    public void alterar(CategoriaInventario categoria, String codigo, String nome, boolean exclusivoSecretaria,
            int ordem) {
        this.categoria = categoria;
        this.codigo = codigo;
        this.nome = nome;
        this.exclusivoSecretaria = exclusivoSecretaria;
        this.ordem = (short) ordem;
    }

    /** Grupo inativo sai do formulário; o que os estabelecimentos marcaram fica guardado. */
    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getSegmento() { return segmento; }
    public CategoriaInventario getCategoria() { return categoria; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public boolean isExclusivoSecretaria() { return exclusivoSecretaria; }
    public Short getOrdem() { return ordem; }
    public boolean isAtivo() { return ativo; }
}
