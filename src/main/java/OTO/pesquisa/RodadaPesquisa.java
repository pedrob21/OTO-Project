package OTO.pesquisa;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pesquisa enviada aos parceiros (ex.: 005/26), com uma ou mais campanhas. */
@Entity
@Table(name = "rodada_pesquisa")
public class RodadaPesquisa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;

    private String titulo;

    @Enumerated(EnumType.STRING)
    private StatusRodada status;

    @Column(name = "data_abertura")
    private LocalDate dataAbertura;

    @Column(name = "prazo_resposta")
    private LocalDate prazoResposta;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    @Column(name = "criado_por", updatable = false)
    private Long criadoPor;

    @Column(name = "criado_em", updatable = false)
    private Instant criadoEm;

    protected RodadaPesquisa() {
    }

    public RodadaPesquisa(String codigo, String titulo, LocalDate dataAbertura, LocalDate prazoResposta, Long criadoPor) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.dataAbertura = dataAbertura;
        this.prazoResposta = prazoResposta;
        this.status = StatusRodada.RASCUNHO;
        this.criadoPor = criadoPor;
        this.criadoEm = Instant.now();
    }

    public void alterar(String codigo, String titulo, LocalDate dataAbertura, LocalDate prazoResposta) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.dataAbertura = dataAbertura;
        this.prazoResposta = prazoResposta;
    }

    public void abrir(LocalDate dataAbertura, LocalDate prazoResposta) {
        this.dataAbertura = dataAbertura;
        this.prazoResposta = prazoResposta;
        this.status = StatusRodada.ABERTA;
    }

    public void prorrogar(LocalDate novoPrazo) {
        this.prazoResposta = novoPrazo;
    }

    public void encerrar() {
        this.status = StatusRodada.ENCERRADA;
        this.encerradaEm = Instant.now();
    }

    /** Volta a receber respostas (encerramento por engano ou respostas atrasadas). */
    public void reabrir(LocalDate novoPrazo) {
        this.status = StatusRodada.ABERTA;
        this.encerradaEm = null;
        this.prazoResposta = novoPrazo;
    }

    public boolean isRascunho() { return status == StatusRodada.RASCUNHO; }
    public boolean isAberta() { return status == StatusRodada.ABERTA; }
    public boolean isEncerrada() { return status == StatusRodada.ENCERRADA; }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public StatusRodada getStatus() { return status; }
    public LocalDate getDataAbertura() { return dataAbertura; }
    public LocalDate getPrazoResposta() { return prazoResposta; }
    public Instant getEncerradaEm() { return encerradaEm; }
    public Long getCriadoPor() { return criadoPor; }
    public Instant getCriadoEm() { return criadoEm; }
}
