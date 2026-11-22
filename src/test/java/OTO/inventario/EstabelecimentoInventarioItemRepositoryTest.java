package OTO.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.estabelecimento.Estabelecimento;
import OTO.usuario.Usuario;

class EstabelecimentoInventarioItemRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private EstabelecimentoInventarioItemRepository repository;

    private Usuario autor;
    private Estabelecimento alfa;
    private Estabelecimento beta;
    private InventarioItem piscina;
    private InventarioItem outros;
    private InventarioItem sauna;

    private void cenario() {
        autor = usuario("autor@oto.com");
        alfa = estabelecimento("Alfa", "HOTEL", "11111111000101");
        beta = estabelecimento("Beta", "HOTEL", "11111111000102");
        InventarioGrupo g = gravar(new InventarioGrupo(CategoriaInventario.SERVICOS_EQUIPAMENTOS, "TESTE_G", "Grupo", false, 900));
        piscina = gravar(new InventarioItem(g, "PISCINA", "Piscina", null, false, 1));
        outros = gravar(new InventarioItem(g, "OUTROS", "Outros", null, true, 2));
        sauna = gravar(new InventarioItem(g, "SAUNA", "Sauna", null, false, 3));
    }

    private EstabelecimentoInventarioItem marca(Estabelecimento e, InventarioItem i, String complemento) {
        return gravar(new EstabelecimentoInventarioItem(e.getId(), i.getId(), complemento, autor.getId()));
    }

    @Test
    void itensMarcadosSaoDoEstabelecimentoPedido() {
        cenario();
        marca(alfa, piscina, null);
        marca(alfa, outros, "Quadra de areia");
        marca(beta, sauna, null);
        gravarELimpar();

        assertThat(repository.findByIdEstabelecimentoId(alfa.getId()))
                .extracting(EstabelecimentoInventarioItem::getItemId)
                .containsExactlyInAnyOrder(piscina.getId(), outros.getId());
    }

    @Test
    void itemMarcadoPorAlgumEstabelecimentoEhDetectado() {
        cenario();
        marca(beta, sauna, null);
        gravarELimpar();

        assertThat(repository.existsByIdItemId(sauna.getId())).isTrue();
        assertThat(repository.existsByIdItemId(piscina.getId())).isFalse();
    }

    @Test
    void complementoSoContaQuandoHaTextoLivreGravado() {
        cenario();
        marca(alfa, piscina, null);
        marca(alfa, outros, "Quadra de areia");
        gravarELimpar();

        assertThat(repository.existsByIdItemIdAndComplementoIsNotNull(outros.getId())).isTrue();
        assertThat(repository.existsByIdItemIdAndComplementoIsNotNull(piscina.getId())).isFalse();
        assertThat(repository.existsByIdItemIdAndComplementoIsNotNull(sauna.getId())).isFalse();
    }

    @Test
    void itemInexistenteViolaAChaveEstrangeira() {
        cenario();

        assertThatThrownBy(() -> gravar(new EstabelecimentoInventarioItem(alfa.getId(), -1L, null, autor.getId())))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("estabelecimento_inventario_item_item_id_fkey");
    }
}
