package OTO.estabelecimento;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import OTO.Fixtures;

class EstabelecimentoComplementarTest {

    @Test
    void outroSegmentoNaoEntraNoCalculo() {
        Estabelecimento e = Fixtures.estabelecimento(1, "A", "HOTEL");
        assertThat(e.isConsideradoNoCalculo()).isTrue();

        ReflectionTestUtils.setField(e, "segmento", "AGENCIA");

        assertThat(e.isConsideradoNoCalculo()).isFalse();
    }

    @Test
    void construtorDoJpa() {
        assertThat(new Estabelecimento().getSegmento()).isNull();
    }
}
