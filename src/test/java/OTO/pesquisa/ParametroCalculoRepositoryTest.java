package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.usuario.Usuario;

/** A migration V7 já grava um parâmetro com vigência em 2024-01-01 (mínimo de 70% dos leitos). */
class ParametroCalculoRepositoryTest extends BaseRepositorioTest {

    private static final LocalDate DA_MIGRATION = LocalDate.of(2024, 1, 1);
    private static final LocalDate JAN_2026 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JUL_2026 = LocalDate.of(2026, 7, 1);

    @Autowired
    private ParametroCalculoRepository repository;

    private Usuario autor;

    private ParametroCalculo parametro(LocalDate desde, String minimoLeitos) {
        return gravar(new ParametroCalculo(desde, new BigDecimal("15.15"), 4, 2, new BigDecimal("250.00"),
                new BigDecimal("105.00"), new BigDecimal(minimoLeitos), "Pesquisa de demanda", autor.getId()));
    }

    private void cenario() {
        autor = usuario("autor@oto.com");
        parametro(JAN_2026, "40.00");
        parametro(JUL_2026, "50.00");
        gravarELimpar();
    }

    @Test
    void vigenteNoDiaExatoDaMudancaUsaOParametroNovo() {
        cenario();

        assertThat(repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JUL_2026).orElseThrow()
                .getPercentualMinimoLeitosPublicacao()).isEqualByComparingTo("50.00");
    }

    @Test
    void vigenteNoDiaAnteriorUsaOParametroAntigo() {
        cenario();

        assertThat(repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JUL_2026.minusDays(1))
                .orElseThrow().getPercentualMinimoLeitosPublicacao()).isEqualByComparingTo("40.00");
    }

    @Test
    void naoUsaParametroFuturoEFicaSemParametroAntesDoPrimeiro() {
        cenario();

        assertThat(repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(DA_MIGRATION)
                .orElseThrow().getPercentualMinimoLeitosPublicacao()).isEqualByComparingTo("70.00");
        assertThat(repository.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(DA_MIGRATION.minusDays(1)))
                .isEmpty();
    }

    @Test
    void listaDaVigenciaMaisRecenteParaAMaisAntigaIncluindoOParametroDaMigration() {
        cenario();

        assertThat(repository.findAllByOrderByVigenteDesdeDesc()).extracting(ParametroCalculo::getVigenteDesde)
                .containsExactly(JUL_2026, JAN_2026, DA_MIGRATION);
    }

    @Test
    void existePorDataDeVigencia() {
        cenario();

        assertThat(repository.existsByVigenteDesde(DA_MIGRATION)).isTrue();
        assertThat(repository.existsByVigenteDesde(JAN_2026)).isTrue();
        assertThat(repository.existsByVigenteDesde(JAN_2026.plusDays(1))).isFalse();
    }

    @Test
    void vigenciaRepetidaViolaAUnicidade() {
        cenario();

        assertThatThrownBy(() -> parametro(JAN_2026, "10.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_parametro_calculo_vigencia");
    }

    @Test
    void dayUseDeCemPorCentoEhRejeitado() {
        autor = usuario("autor@oto.com");

        assertThatThrownBy(() -> gravar(new ParametroCalculo(JAN_2026, new BigDecimal("100.00"), 4, 2,
                new BigDecimal("250.00"), new BigDecimal("105.00"), new BigDecimal("30.00"), "Fonte", autor.getId())))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_parametro_calculo_percentuais");
    }

    @Test
    void diasEmParquesNaoPodemPassarDosDiasDePermanencia() {
        autor = usuario("autor@oto.com");

        assertThatThrownBy(() -> gravar(new ParametroCalculo(JAN_2026, new BigDecimal("15.00"), 4, 5,
                new BigDecimal("250.00"), new BigDecimal("105.00"), new BigDecimal("30.00"), "Fonte", autor.getId())))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_parametro_calculo_dias");
    }
}
