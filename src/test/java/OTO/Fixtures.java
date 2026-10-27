package OTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.CapacidadeHospedagem;
import OTO.estabelecimento.DadosCadastrais;
import OTO.estabelecimento.DadosSecretaria;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.RhcResumo;
import OTO.estabelecimento.Situacao;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.DadosCampanha;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.ParametroCalculo;
import OTO.pesquisa.RespostaOcupacao;
import OTO.pesquisa.RodadaPesquisa;
import OTO.pesquisa.StatusRodada;
import OTO.pesquisa.TipoPeriodo;
import OTO.usuario.Perfil;
import OTO.usuario.Usuario;

/**
 * Objetos de domínio prontos para os testes unitários. As entidades não têm setter de id
 * (o banco gera), então o id é preenchido por reflexão.
 */
public final class Fixtures {

    public static final UsuarioLogado ADMIN = new UsuarioLogado(1L, Perfil.ADMIN);
    public static final UsuarioLogado PARCEIRO = new UsuarioLogado(2L, Perfil.PARCEIRO);
    public static final UsuarioLogado VISUALIZADOR = new UsuarioLogado(3L, Perfil.VISUALIZADOR);

    private Fixtures() {
    }

    public static <T> T comId(T entidade, Object id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    public static Jwt jwt(long usuarioId, Perfil perfil) {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject(String.valueOf(usuarioId))
                .claim("perfil", perfil.name()).build();
    }

    public static Usuario usuario(long id, String nome, String email, Perfil perfil) {
        return comId(new Usuario(nome, email, perfil), id);
    }

    public static DadosCadastrais dados(String nomeFantasia) {
        return dados(nomeFantasia, null);
    }

    public static DadosCadastrais dados(String nomeFantasia, String cnpj) {
        return new DadosCadastrais(nomeFantasia, null, cnpj, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, false, null, null, null, null, null, null, null, null, null);
    }

    public static DadosSecretaria secretaria(String tipo) {
        return secretaria(tipo, Situacao.REGULAR);
    }

    public static DadosSecretaria secretaria(String tipo, Situacao situacao) {
        return new DadosSecretaria(tipo, situacao, "Maria", true, "17999990000", "pesquisa@hotel.com", false, false);
    }

    public static Estabelecimento estabelecimento(long id, String nome, String tipo) {
        return estabelecimento(id, nome, tipo, Situacao.REGULAR);
    }

    public static Estabelecimento estabelecimento(long id, String nome, String tipo, Situacao situacao) {
        return comId(new Estabelecimento(dados(nome), secretaria(tipo, situacao)), id);
    }

    public static CapacidadeHospedagem capacidade(long id, Estabelecimento e, LocalDate desde, int uh, int leitos,
            int maxPorUh) {
        return comId(new CapacidadeHospedagem(e, desde, uh, 0, leitos, maxPorUh, 1L), id);
    }

    public static RhcResumo rhc(long id, LocalDate desde, int registros, int uh, int leitos, int maxPorUh) {
        return comId(new RhcResumo(desde, registros, uh, leitos, maxPorUh, 1L), id);
    }

    public static RodadaPesquisa rodada(long id, String codigo, StatusRodada status) {
        RodadaPesquisa r = comId(new RodadaPesquisa(codigo, "Pesquisa " + codigo, null, null, 1L), id);
        if (status != StatusRodada.RASCUNHO) {
            r.abrir(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));
        }
        if (status == StatusRodada.ENCERRADA) {
            r.encerrar();
        }
        return r;
    }

    public static DadosCampanha dadosMensal(String nome, Modalidade modalidade, YearMonth mes, int diarias) {
        return new DadosCampanha(nome, modalidade, TipoPeriodo.MENSAL, null, mes.atDay(1), mes.atEndOfMonth(), diarias);
    }

    public static Campanha campanhaMensal(long id, RodadaPesquisa rodada, YearMonth mes) {
        return campanhaMensal(id, rodada, mes, Modalidade.CONSOLIDADA);
    }

    public static Campanha campanhaMensal(long id, RodadaPesquisa rodada, YearMonth mes, Modalidade modalidade) {
        return comId(new Campanha(rodada, dadosMensal("Campanha " + mes, modalidade, mes, mes.lengthOfMonth())), id);
    }

    public static Campanha campanhaFeriado(long id, RodadaPesquisa rodada, String feriado, LocalDate inicio,
            LocalDate fim, Modalidade modalidade) {
        return comId(new Campanha(rodada, new DadosCampanha(feriado + " " + inicio.getYear(), modalidade,
                TipoPeriodo.FERIADO, feriado, inicio, fim, (int) (fim.toEpochDay() - inicio.toEpochDay()))), id);
    }

    public static RespostaOcupacao resposta(long id, Campanha campanha, Estabelecimento e, String taxa, String diaria) {
        return comId(new RespostaOcupacao(campanha, e, new BigDecimal(taxa), diaria == null ? null : new BigDecimal(diaria),
                2L), id);
    }

    /** Parâmetros da planilha: day use 15,15%, 4 dias, 2 em parques, R$ 250 e R$ 105, mínimo de 30% dos leitos. */
    public static ParametroCalculo parametro(long id, LocalDate desde) {
        return comId(new ParametroCalculo(desde, new BigDecimal("15.15"), 4, 2, new BigDecimal("250.00"),
                new BigDecimal("105.00"), new BigDecimal("30.00"), "Pesquisa de demanda", 1L), id);
    }
}
