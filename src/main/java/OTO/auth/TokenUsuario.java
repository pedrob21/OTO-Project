package OTO.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** entidade do token de uso único (convite ou redefinição de senha). Só o hash é gravado. */
@Entity
@Table(name = "token_usuario")
public class TokenUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    private TipoToken tipo;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "criado_em", updatable = false)
    private Instant criadoEm;

    protected TokenUsuario() {
    }

    public TokenUsuario(Long usuarioId, TipoToken tipo, String tokenHash, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
        this.criadoEm = Instant.now();
    }

    public boolean isValido(Instant agora) {
        return usadoEm == null && expiraEm.isAfter(agora);
    }

    public void marcarUsado() {
        this.usadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public TipoToken getTipo() { return tipo; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getUsadoEm() { return usadoEm; }
}
