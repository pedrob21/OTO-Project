package OTO.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;

class InventarioGrupoRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private InventarioGrupoRepository repository;

    private InventarioGrupo grupo(String codigo) {
        return gravar(new InventarioGrupo(CategoriaInventario.ACESSIBILIDADE, codigo, "Grupo " + codigo, false, 900));
    }

    @Test
    void existePorSegmentoECodigo() {
        grupo("TESTE_A");
        gravarELimpar();

        assertThat(repository.existsBySegmentoAndCodigo("MEIO_HOSPEDAGEM", "TESTE_A")).isTrue();
        assertThat(repository.existsBySegmentoAndCodigo("MEIO_HOSPEDAGEM", "TESTE_B")).isFalse();
        assertThat(repository.existsBySegmentoAndCodigo("ALIMENTACAO", "TESTE_A")).isFalse();
    }

    @Test
    void existePorCodigoIgnoraOProprioIdMasVeOutros() {
        InventarioGrupo a = grupo("TESTE_A");
        InventarioGrupo b = grupo("TESTE_B");
        gravarELimpar();

        assertThat(repository.existsBySegmentoAndCodigoAndIdNot("MEIO_HOSPEDAGEM", "TESTE_A", a.getId())).isFalse();
        assertThat(repository.existsBySegmentoAndCodigoAndIdNot("MEIO_HOSPEDAGEM", "TESTE_A", b.getId())).isTrue();
    }

    @Test
    void codigoRepetidoNoSegmentoViolaAUnicidade() {
        grupo("TESTE_A");

        assertThatThrownBy(() -> grupo("TESTE_A")).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_inventario_grupo_codigo");
    }
}
