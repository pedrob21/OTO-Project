package OTO.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.PersistenceException;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;

/** O catálogo real vem da migration V8; os testes usam grupos com código iniciado em TESTE_ e ordem acima de 900. */
class InventarioItemRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private InventarioItemRepository repository;

    private InventarioGrupo grupo(String codigo, int ordem, boolean ativo) {
        InventarioGrupo g = new InventarioGrupo(CategoriaInventario.SERVICOS_EQUIPAMENTOS, codigo, "Grupo " + codigo,
                false, ordem);
        g.definirAtivo(ativo);
        return gravar(g);
    }

    private InventarioItem item(InventarioGrupo g, String codigo, int ordem, boolean ativo) {
        InventarioItem i = new InventarioItem(g, codigo, "Item " + codigo, null, false, ordem);
        i.definirAtivo(ativo);
        return gravar(i);
    }

    @Test
    void catalogoAtivoOrdenaPorGrupoEDepoisPorItemEIgnoraInativos() {
        InventarioGrupo segundo = grupo("TESTE_SEGUNDO", 902, true);
        InventarioGrupo primeiro = grupo("TESTE_PRIMEIRO", 901, true);
        InventarioGrupo inativo = grupo("TESTE_INATIVO", 903, false);
        item(segundo, "S1", 1, true);
        item(primeiro, "P2", 2, true);
        item(primeiro, "P1", 1, true);
        item(primeiro, "P3_INATIVO", 3, false);
        item(inativo, "I1", 1, true);
        gravarELimpar();

        var catalogo = repository.findCatalogoAtivo().stream()
                .filter(i -> i.getGrupo().getCodigo().startsWith("TESTE_")).toList();

        assertThat(catalogo).extracting(InventarioItem::getCodigo).containsExactly("P1", "P2", "S1");
        assertThat(catalogo).allSatisfy(i -> assertThat(Hibernate.isInitialized(i.getGrupo())).isTrue());
    }

    @Test
    void catalogoAtivoIncluiOsItensDaMigrationAntesDosGruposDeTeste() {
        InventarioGrupo g = grupo("TESTE_FIM", 950, true);
        item(g, "F1", 1, true);
        gravarELimpar();

        var catalogo = repository.findCatalogoAtivo();

        assertThat(catalogo).hasSizeGreaterThan(1);
        assertThat(catalogo.get(catalogo.size() - 1).getCodigo()).isEqualTo("F1");
    }

    @Test
    void itensDoGrupoVemPorOrdemEDesempatamPeloId() {
        InventarioGrupo g = grupo("TESTE_G", 901, true);
        InventarioGrupo outro = grupo("TESTE_H", 902, true);
        InventarioItem b = item(g, "B", 2, true);
        InventarioItem a1 = item(g, "A1", 1, true);
        InventarioItem a2 = item(g, "A2", 1, true);
        item(outro, "X", 1, true);
        gravarELimpar();

        assertThat(repository.findByGrupoIdOrderByOrdemAscIdAsc(g.getId())).extracting(InventarioItem::getId)
                .containsExactly(a1.getId(), a2.getId(), b.getId());
    }

    @Test
    void grupoTemItensSoQuandoAlgumItemFoiCadastrado() {
        InventarioGrupo comItem = grupo("TESTE_COM", 901, true);
        InventarioGrupo semItem = grupo("TESTE_SEM", 902, true);
        item(comItem, "A", 1, false);
        gravarELimpar();

        assertThat(repository.existsByGrupoId(comItem.getId())).isTrue();
        assertThat(repository.existsByGrupoId(semItem.getId())).isFalse();
    }

    @Test
    void codigoDoItemEhUnicoSoDentroDoGrupo() {
        InventarioGrupo g1 = grupo("TESTE_G1", 901, true);
        InventarioGrupo g2 = grupo("TESTE_G2", 902, true);
        InventarioItem piscina = item(g1, "PISCINA", 1, true);
        item(g2, "PISCINA", 1, true);
        gravarELimpar();

        assertThat(repository.existsByGrupoIdAndCodigo(g1.getId(), "PISCINA")).isTrue();
        assertThat(repository.existsByGrupoIdAndCodigo(g1.getId(), "SAUNA")).isFalse();
        assertThat(repository.existsByGrupoIdAndCodigoAndIdNot(g1.getId(), "PISCINA", piscina.getId())).isFalse();
        assertThat(repository.existsByGrupoIdAndCodigoAndIdNot(g2.getId(), "PISCINA", piscina.getId())).isTrue();
    }

    @Test
    void codigoRepetidoNoMesmoGrupoViolaAUnicidade() {
        InventarioGrupo g = grupo("TESTE_G", 901, true);
        item(g, "PISCINA", 1, true);

        assertThatThrownBy(() -> item(g, "PISCINA", 2, true)).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_inventario_item_codigo");
    }
}
