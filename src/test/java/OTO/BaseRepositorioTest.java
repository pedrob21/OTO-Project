package OTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.Situacao;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.RespostaOcupacao;
import OTO.pesquisa.RodadaPesquisa;
import OTO.pesquisa.StatusRodada;
import OTO.usuario.Perfil;
import OTO.usuario.Usuario;

/**
 * Base dos testes de repositório: {@code @DataJpaTest} contra o PostgreSQL real (Testcontainers),
 * com as migrations do Flyway. Cada teste roda numa transação que é desfeita no fim.
 * As tabelas de referência e as cargas das migrations V7 e V8 já vêm preenchidas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(BancoDeTesteRepositorio.class)
public abstract class BaseRepositorioTest {

    @Autowired
    protected TestEntityManager em;

    protected void gravarELimpar() {
        em.flush();
        em.clear();
    }

    protected <T> T gravar(T entidade) {
        return em.persistAndFlush(entidade);
    }

    protected Usuario usuario(String email) {
        return gravar(new Usuario("Usuário " + email, email, Perfil.ADMIN));
    }

    protected Estabelecimento estabelecimento(String nome, String tipo, String cnpj) {
        return estabelecimento(nome, tipo, cnpj, Situacao.REGULAR);
    }

    protected Estabelecimento estabelecimento(String nome, String tipo, String cnpj, Situacao situacao) {
        return gravar(new Estabelecimento(Fixtures.dados(nome, cnpj), Fixtures.secretaria(tipo, situacao)));
    }

    protected RodadaPesquisa rodada(String codigo, StatusRodada status, Usuario criador) {
        RodadaPesquisa r = new RodadaPesquisa(codigo, "Pesquisa " + codigo, null, null, criador.getId());
        if (status != StatusRodada.RASCUNHO) {
            r.abrir(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));
        }
        if (status == StatusRodada.ENCERRADA) {
            r.encerrar();
        }
        return gravar(r);
    }

    protected Campanha campanhaMensal(RodadaPesquisa rodada, YearMonth mes, Modalidade modalidade) {
        return gravar(new Campanha(rodada, Fixtures.dadosMensal("Campanha " + mes + " " + modalidade, modalidade, mes,
                mes.lengthOfMonth())));
    }

    protected RespostaOcupacao resposta(Campanha campanha, Estabelecimento e, String taxa, Usuario autor) {
        return gravar(new RespostaOcupacao(campanha, e, new BigDecimal(taxa), null, autor.getId()));
    }

    protected static void definirCampo(Object alvo, String campo, Object valor) {
        ReflectionTestUtils.setField(alvo, campo, valor);
    }
}
