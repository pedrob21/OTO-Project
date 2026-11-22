package OTO.estabelecimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.usuario.Usuario;

class RhcResumoRepositoryTest extends BaseRepositorioTest {

    private static final LocalDate JAN = LocalDate.of(2025, 1, 1);
    private static final LocalDate JUL = LocalDate.of(2025, 7, 1);

    @Autowired
    private RhcResumoRepository repository;

    private Usuario autor;

    private RhcResumo rhc(LocalDate desde, int registros) {
        return gravar(new RhcResumo(desde, registros, registros * 2, registros * 6, 4, autor.getId()));
    }

    private void cenario() {
        autor = usuario("autor@oto.com");
        rhc(JUL, 80);
        rhc(JAN, 50);
        gravarELimpar();
    }

    @Test
    void listaDaVigenciaMaisRecenteParaAMaisAntiga() {
        cenario();

        assertThat(repository.findAllByOrderByVigenteDesdeDesc()).extracting(RhcResumo::getVigenteDesde)
                .containsExactly(JUL, JAN);
    }

    @Test
    void vigenteNoDiaExatoDaMudancaUsaOResumoNovo() {
        cenario();

        assertThat(repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JUL).orElseThrow()
                .getQtdRegistros()).isEqualTo(80);
    }

    @Test
    void vigenteNoDiaAnteriorUsaOResumoAntigo() {
        cenario();

        assertThat(repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JUL.minusDays(1))
                .orElseThrow().getQtdRegistros()).isEqualTo(50);
    }

    @Test
    void antesDaPrimeiraVigenciaNaoHaResumo() {
        cenario();

        assertThat(repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JAN.minusDays(1)))
                .isEmpty();
    }

    @Test
    void existePorDataDeVigencia() {
        cenario();

        assertThat(repository.existsByVigenteDesde(JAN)).isTrue();
        assertThat(repository.existsByVigenteDesde(JAN.plusDays(1))).isFalse();
    }

    @Test
    void vigenciaRepetidaViolaAUnicidade() {
        cenario();

        assertThatThrownBy(() -> rhc(JAN, 10)).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_rhc_resumo_vigencia");
    }

    @Test
    void capacidadeMaximaPorUhPrecisaSerPositiva() {
        autor = usuario("autor@oto.com");

        assertThatThrownBy(() -> gravar(new RhcResumo(JAN, 10, 20, 60, 0, autor.getId())))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_rhc_resumo_valores");
    }
}
