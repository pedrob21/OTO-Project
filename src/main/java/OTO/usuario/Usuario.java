package OTO.usuario;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import OTO.estabelecimento.Estabelecimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String email;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    private Perfil perfil;

    private boolean ativo;

    @Column(name = "ultimo_acesso_em")
    private Instant ultimoAcessoEm;

    @Column(name = "criado_em", updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    /** Estabelecimentos que o usuário PARCEIRO representa. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_estabelecimento",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "estabelecimento_id"))
    private Set<Estabelecimento> estabelecimentos = new HashSet<>();

    protected Usuario() {
    }

    public Usuario(String nome, String email, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.ativo = true;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }

    public void alterarDados(String nome, Perfil perfil) {
        this.nome = nome;
        this.perfil = perfil;
        this.atualizadoEm = Instant.now();
    }

    public void definirSenha(String senhaHash) {
        this.senhaHash = senhaHash;
        this.atualizadoEm = Instant.now();
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
        this.atualizadoEm = Instant.now();
    }

    public void registrarAcesso() {
        this.ultimoAcessoEm = Instant.now();
    }

    public void substituirEstabelecimentos(Set<Estabelecimento> novos) {
        this.estabelecimentos.clear();
        this.estabelecimentos.addAll(novos);
        this.atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
    public Instant getUltimoAcessoEm() { return ultimoAcessoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Set<Estabelecimento> getEstabelecimentos() { return estabelecimentos; }
}
