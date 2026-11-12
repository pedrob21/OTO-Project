package OTO.comum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import OTO.comum.excecao.RegraNegocioException;

class CodigosTest {

    @Test
    void deNomeTiraAcentosEPoeSublinhados() {
        assertThat(Codigos.deNome("Área de lazer", 60)).isEqualTo("AREA_DE_LAZER");
        assertThat(Codigos.deNome("  --Wi-Fi grátis!! ", 60)).isEqualTo("WI_FI_GRATIS");
        assertThat(Codigos.deNome("Café 24h", 60)).isEqualTo("CAFE_24H");
    }

    @Test
    void deNomeCortaNoTamanhoSemTerminarEmSublinhado() {
        assertThat(Codigos.deNome("Abc def", 4)).isEqualTo("ABC");
        assertThat(Codigos.deNome("Piscina aquecida", 7)).isEqualTo("PISCINA");
    }

    @Test
    void livreDevolveOCodigoDoNomeQuandoNaoExiste() {
        assertThat(Codigos.livre("Sauna", 20, c -> false)).isEqualTo("SAUNA");
    }

    @Test
    void livreAcrescentaSufixoAteAcharUmCodigoLivre() {
        Set<String> existentes = Set.of("SAUNA", "SAUNA_2");
        assertThat(Codigos.livre("Sauna", 20, existentes::contains)).isEqualTo("SAUNA_3");
    }

    @Test
    void livreCortaABaseParaCaberOSufixo() {
        assertThat(Codigos.livre("ABCDE", 5, "ABCDE"::equals)).isEqualTo("ABC_2");
    }

    @Test
    void livreSemLetrasNoNomeExigeOCodigo() {
        assertThatThrownBy(() -> Codigos.livre("!!!", 20, c -> false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Informe o código");
    }

    @Test
    void informadoPassaParaMaiusculas() {
        assertThat(Codigos.informado("  area_1 ", 20)).isEqualTo("AREA_1");
    }

    @Test
    void informadoRecusaAcentoEspacoETamanho() {
        assertThatThrownBy(() -> Codigos.informado("área", 20)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("até 20 caracteres");
        assertThatThrownBy(() -> Codigos.informado("A B", 20)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> Codigos.informado("ABCDEF", 5)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void vazio() {
        assertThat(Codigos.vazio(null)).isTrue();
        assertThat(Codigos.vazio("   ")).isTrue();
        assertThat(Codigos.vazio("x")).isFalse();
    }
}
