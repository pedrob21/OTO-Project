package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;

class CampanhaDiariaTest {

    @Test
    void diariaGeralNaoTemTipo() {
        CampanhaDiaria geral = Fixtures.comId(new CampanhaDiaria(10L, null, new BigDecimal("300"), "Pesquisa", 1L), 5L);

        assertThat(geral.getId()).isEqualTo(5L);
        assertThat(geral.isGeral()).isTrue();
        assertThat(geral.getRegistradoEm()).isNotNull();
    }

    @Test
    void construtorDoJpa() {
        CampanhaDiaria vazia = new CampanhaDiaria();
        assertThat(vazia.getId()).isNull();
        assertThat(vazia.getDiariaMedia()).isNull();
    }
}
