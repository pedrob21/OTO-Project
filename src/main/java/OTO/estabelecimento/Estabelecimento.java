package OTO.estabelecimento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Empresa do trade turístico (hoje, só meios de hospedagem) com os dados do
 * inventário oficial. Os campos são lidos e alterados em blocos
 * ({@link DadosCadastrais} e {@link DadosSecretaria}).
 */
@Entity
@Table(name = "estabelecimento")
public class Estabelecimento {

    public static final String SEGMENTO_MEIO_HOSPEDAGEM = "MEIO_HOSPEDAGEM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String segmento;

    @Column(name = "tipo_hospedagem_codigo")
    private String tipoHospedagemCodigo;

    @Enumerated(EnumType.STRING)
    private Situacao situacao;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;
    @Column(name = "razao_social")
    private String razaoSocial;
    private String cnpj;
    @Column(name = "data_abertura")
    private LocalDate dataAbertura;
    private String proprietarios;
    private String cnae;

    private String endereco;
    private String bairro;
    private String regiao;
    private String localizacao;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "telefone_comercial")
    private String telefoneComercial;
    @Column(name = "email_comercial")
    private String emailComercial;
    private String site;
    private String facebook;
    private String instagram;

    @Enumerated(EnumType.STRING)
    @Column(name = "cadastur_exigencia")
    private CadasturExigencia cadasturExigencia;
    @Column(name = "possui_cadastur")
    private boolean possuiCadastur;
    @Column(name = "cadastur_numero")
    private String cadasturNumero;
    @Column(name = "cadastur_validade")
    private LocalDate cadasturValidade;

    @Column(name = "funcionarios_fixos")
    private Integer funcionariosFixos;
    @Column(name = "funcionarios_temporarios")
    private Integer funcionariosTemporarios;
    @Column(name = "funcionamento_24h")
    private Boolean funcionamento24h;
    @Column(name = "horario_check_in")
    private LocalTime horarioCheckIn;
    @Column(name = "horario_check_out")
    private LocalTime horarioCheckOut;
    @Column(name = "formas_reserva")
    private String formasReserva;
    @Column(name = "formas_pagamento")
    private String formasPagamento;

    @Column(name = "contato_nome")
    private String contatoNome;
    @Column(name = "aceita_pesquisas")
    private boolean aceitaPesquisas;
    @Column(name = "telefone_pesquisas")
    private String telefonePesquisas;
    @Column(name = "email_pesquisas")
    private String emailPesquisas;
    @Column(name = "destino_inteligente")
    private boolean destinoInteligente;
    @Column(name = "dados_pddt")
    private boolean dadosPddt;

    private boolean ativo;
    @Column(name = "inventario_atualizado_em")
    private Instant inventarioAtualizadoEm;
    @Column(name = "criado_em", updatable = false)
    private Instant criadoEm;
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    protected Estabelecimento() {
    }

    public Estabelecimento(DadosCadastrais dados, DadosSecretaria secretaria) {
        this.segmento = SEGMENTO_MEIO_HOSPEDAGEM;
        this.ativo = true;
        this.criadoEm = Instant.now();
        atualizar(dados);
        atualizar(secretaria);
    }

    public void atualizar(DadosCadastrais d) {
        this.nomeFantasia = d.nomeFantasia();
        this.razaoSocial = d.razaoSocial();
        this.cnpj = d.cnpj();
        this.dataAbertura = d.dataAbertura();
        this.proprietarios = d.proprietarios();
        this.cnae = d.cnae();
        this.endereco = d.endereco();
        this.bairro = d.bairro();
        this.regiao = d.regiao();
        this.localizacao = d.localizacao();
        this.latitude = d.latitude();
        this.longitude = d.longitude();
        this.telefoneComercial = d.telefoneComercial();
        this.emailComercial = d.emailComercial();
        this.site = d.site();
        this.facebook = d.facebook();
        this.instagram = d.instagram();
        this.cadasturExigencia = d.cadasturExigencia();
        this.possuiCadastur = d.possuiCadastur();
        this.cadasturNumero = d.cadasturNumero();
        this.cadasturValidade = d.cadasturValidade();
        this.funcionariosFixos = d.funcionariosFixos();
        this.funcionariosTemporarios = d.funcionariosTemporarios();
        this.funcionamento24h = d.funcionamento24h();
        this.horarioCheckIn = d.horarioCheckIn();
        this.horarioCheckOut = d.horarioCheckOut();
        this.formasReserva = d.formasReserva();
        this.formasPagamento = d.formasPagamento();
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(DadosSecretaria s) {
        this.tipoHospedagemCodigo = s.tipoHospedagemCodigo();
        this.situacao = s.situacao();
        this.contatoNome = s.contatoNome();
        this.aceitaPesquisas = s.aceitaPesquisas();
        this.telefonePesquisas = s.telefonePesquisas();
        this.emailPesquisas = s.emailPesquisas();
        this.destinoInteligente = s.destinoInteligente();
        this.dadosPddt = s.dadosPddt();
        this.atualizadoEm = Instant.now();
    }

    public DadosCadastrais getDadosCadastrais() {
        return new DadosCadastrais(nomeFantasia, razaoSocial, cnpj, dataAbertura, proprietarios, cnae,
                endereco, bairro, regiao, localizacao, latitude, longitude,
                telefoneComercial, emailComercial, site, facebook, instagram,
                cadasturExigencia, possuiCadastur, cadasturNumero, cadasturValidade,
                funcionariosFixos, funcionariosTemporarios, funcionamento24h,
                horarioCheckIn, horarioCheckOut, formasReserva, formasPagamento);
    }

    public DadosSecretaria getDadosSecretaria() {
        return new DadosSecretaria(tipoHospedagemCodigo, situacao, contatoNome, aceitaPesquisas,
                telefonePesquisas, emailPesquisas, destinoInteligente, dadosPddt);
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
        this.atualizadoEm = Instant.now();
    }

    public void marcarInventarioAtualizado() {
        this.inventarioAtualizadoEm = Instant.now();
    }

    /** Entra no cálculo: meio de hospedagem ativo e regular. */
    public boolean isConsideradoNoCalculo() {
        return ativo && situacao == Situacao.REGULAR && SEGMENTO_MEIO_HOSPEDAGEM.equals(segmento);
    }

    public Long getId() { return id; }
    public String getSegmento() { return segmento; }
    public String getTipoHospedagemCodigo() { return tipoHospedagemCodigo; }
    public Situacao getSituacao() { return situacao; }
    public String getNomeFantasia() { return nomeFantasia; }
    public String getCnpj() { return cnpj; }
    public boolean isAceitaPesquisas() { return aceitaPesquisas; }
    public boolean isAtivo() { return ativo; }
    public Instant getInventarioAtualizadoEm() { return inventarioAtualizadoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
