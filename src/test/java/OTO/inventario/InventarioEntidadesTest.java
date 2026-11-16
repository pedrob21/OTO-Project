package OTO.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;

class InventarioEntidadesTest {

    @Test
    void chaveDoItemMarcadoComparaEstabelecimentoEItem() {
        EstabelecimentoInventarioItemId chave = new EstabelecimentoInventarioItemId(1L, 10L);

        assertThat(chave.getEstabelecimentoId()).isEqualTo(1L);
        assertThat(chave.getItemId()).isEqualTo(10L);
        assertThat(chave).isEqualTo(chave);
        assertThat(chave).isEqualTo(new EstabelecimentoInventarioItemId(1L, 10L));
        assertThat(chave).hasSameHashCodeAs(new EstabelecimentoInventarioItemId(1L, 10L));
        assertThat(chave).isNotEqualTo(new EstabelecimentoInventarioItemId(2L, 10L));
        assertThat(chave).isNotEqualTo(new EstabelecimentoInventarioItemId(1L, 11L));
        assertThat(chave).isNotEqualTo(null);
        assertThat(chave).isNotEqualTo("1-10");
        assertThat(new EstabelecimentoInventarioItemId()).isEqualTo(new EstabelecimentoInventarioItemId());
    }

    @Test
    void itemMarcadoUsaAChaveComposta() {
        EstabelecimentoInventarioItem marcado = new EstabelecimentoInventarioItem(1L, 10L, null, 5L);

        assertThat(marcado.getId()).isEqualTo(new EstabelecimentoInventarioItemId(1L, 10L));
        assertThat(marcado.getItemId()).isEqualTo(10L);
        assertThat(new EstabelecimentoInventarioItem().getId()).isNull();
    }

    @Test
    void grupoNovoEhDoMeioDeHospedagem() {
        InventarioGrupo grupo = Fixtures.comId(new InventarioGrupo(CategoriaInventario.ACESSIBILIDADE, "RAMPA", "Rampa",
                true, 2), 7L);

        assertThat(grupo.getId()).isEqualTo(7L);
        assertThat(grupo.getSegmento()).isEqualTo(InventarioGrupo.SEGMENTO_MEIO_HOSPEDAGEM);
        assertThat(grupo.isExclusivoSecretaria()).isTrue();
        assertThat(new InventarioGrupo().getSegmento()).isNull();
    }
}
