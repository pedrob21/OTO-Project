package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.YearMonth;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.usuario.Usuario;

class CampanhaDiariaRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private CampanhaDiariaRepository repository;

    private Usuario admin;
    private Campanha jan;
    private Campanha fev;

    private CampanhaDiaria diaria(Campanha c, String tipo, String valor) {
        return gravar(new CampanhaDiaria(c.getId(), tipo, new BigDecimal(valor), "Planilha", admin.getId()));
    }

    private void cenario() {
        admin = usuario("admin@oto.com");
        RodadaPesquisa rodada = rodada("001/26", StatusRodada.ABERTA, admin);
        jan = campanhaMensal(rodada, YearMonth.of(2026, 1), Modalidade.CONSOLIDADA);
        fev = campanhaMensal(rodada, YearMonth.of(2026, 2), Modalidade.CONSOLIDADA);
    }

    @Test
    void diariasSaoListadasPorCampanhaIncluindoAGeralSemTipo() {
        cenario();
        diaria(jan, null, "300.00");
        diaria(jan, "HOTEL", "350.00");
        diaria(fev, "HOTEL", "400.00");
        gravarELimpar();

        assertThat(repository.findByCampanhaId(jan.getId()))
                .extracting(CampanhaDiaria::getTipoHospedagemCodigo, d -> d.getDiariaMedia().intValue())
                .containsExactlyInAnyOrder(tuple(null, 300),
                        tuple("HOTEL", 350));
    }

    @Test
    void duasDiariasGeraisNaMesmaCampanhaViolamAUnicidadeComNulosIguais() {
        cenario();
        diaria(jan, null, "300.00");

        assertThatThrownBy(() -> diaria(jan, null, "310.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_campanha_diaria");
    }

    @Test
    void mesmoTipoNaMesmaCampanhaViolaAUnicidadeMasEmOutraCampanhaNao() {
        cenario();
        diaria(jan, "HOTEL", "350.00");
        diaria(fev, "HOTEL", "360.00");

        assertThatThrownBy(() -> diaria(jan, "HOTEL", "370.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_campanha_diaria");
    }

    @Test
    void diariaZeradaEhRejeitada() {
        cenario();

        assertThatThrownBy(() -> diaria(jan, "HOTEL", "0.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_campanha_diaria_valor");
    }

    @Test
    void excluirACampanhaExcluiAsDiariasDela() {
        cenario();
        diaria(jan, null, "300.00");
        diaria(fev, null, "310.00");
        gravarELimpar();

        em.getEntityManager().createNativeQuery("delete from campanha where id = :id")
                .setParameter("id", jan.getId()).executeUpdate();
        em.clear();

        assertThat(repository.findByCampanhaId(jan.getId())).isEmpty();
        assertThat(repository.findByCampanhaId(fev.getId())).hasSize(1);
    }
}
