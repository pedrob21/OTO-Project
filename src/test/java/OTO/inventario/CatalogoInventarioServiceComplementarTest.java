package OTO.inventario;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.CatalogoItem;
import OTO.inventario.InventarioDtos.GrupoRequest;
import OTO.inventario.InventarioDtos.ItemRequest;

/** Caminhos de sucesso com código e ordem informados, inativação e troca de complemento. */
@ExtendWith(MockitoExtension.class)
class CatalogoInventarioServiceComplementarTest {

    @Mock
    private InventarioGrupoRepository grupos;
    @Mock
    private InventarioItemRepository itens;
    @Mock
    private EstabelecimentoInventarioItemRepository marcados;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private CatalogoInventarioService servico;

    private static InventarioGrupo grupo(long id, String codigo, int ordem) {
        return Fixtures.comId(new InventarioGrupo(CategoriaInventario.SERVICOS_EQUIPAMENTOS, codigo, codigo, false, ordem), id);
    }

    private static InventarioItem item(long id, InventarioGrupo grupo, String codigo, boolean complemento, int ordem) {
        return Fixtures.comId(new InventarioItem(grupo, codigo, codigo, null, complemento, ordem), id);
    }

    // ------------------------------------------------------------------ grupos

    @Test
    void alterarGrupoParaCodigoLivreEOrdemInformada() {
        InventarioGrupo lazer = grupo(1, "LAZER", 3);
        when(grupos.findById(1L)).thenReturn(Optional.of(lazer));
        when(grupos.existsBySegmentoAndCodigoAndIdNot(InventarioGrupo.SEGMENTO_MEIO_HOSPEDAGEM, "RECREACAO", 1L))
                .thenReturn(false);
        when(itens.findByGrupoIdOrderByOrdemAscIdAsc(1L)).thenReturn(List.of());

        CatalogoGrupo alterado = servico.alterarGrupo(1L, new GrupoRequest(CategoriaInventario.SERVICOS_EQUIPAMENTOS,
                "RECREACAO", "Recreação", true, 8), ADMIN);

        assertThat(alterado.codigo()).isEqualTo("RECREACAO");
        assertThat(alterado.ordem()).isEqualTo(8);
        assertThat(lazer.isExclusivoSecretaria()).isTrue();
    }

    @Test
    void inativarGrupoAtivo() {
        InventarioGrupo lazer = grupo(1, "LAZER", 3);
        when(grupos.findById(1L)).thenReturn(Optional.of(lazer));

        servico.definirAtivoGrupo(1L, false, ADMIN);
        servico.definirAtivoGrupo(1L, true, ADMIN);

        assertThat(lazer.isAtivo()).isTrue();
        verify(auditoria).registrar(1L, "inventario_grupo", 1L, "INATIVAR", null, null);
        verify(auditoria).registrar(1L, "inventario_grupo", 1L, "REATIVAR", null, null);
    }

    // ------------------------------------------------------------------ itens

    @Test
    void criarItemComCodigoLivreEOrdemInformada() {
        InventarioGrupo lazer = grupo(1, "LAZER", 3);
        when(grupos.findById(1L)).thenReturn(Optional.of(lazer));
        when(itens.existsByGrupoIdAndCodigo(1L, "SAUNA")).thenReturn(false);
        when(itens.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 20L));

        CatalogoItem criado = servico.criarItem(1L, new ItemRequest("SAUNA", " Sauna ", null, false, 4), ADMIN);

        assertThat(criado.id()).isEqualTo(20L);
        assertThat(criado.codigo()).isEqualTo("SAUNA");
        assertThat(criado.ordem()).isEqualTo(4);
        verify(itens, never()).findByGrupoIdOrderByOrdemAscIdAsc(anyLong());
    }

    @Test
    void alterarItemParaCodigoLivreSemOrdem() {
        InventarioItem outros = item(11, grupo(1, "LAZER", 3), "OUTROS", false, 2);
        when(itens.findById(11L)).thenReturn(Optional.of(outros));
        when(itens.existsByGrupoIdAndCodigoAndIdNot(1L, "DIVERSOS", 11L)).thenReturn(false);

        CatalogoItem alterado = servico.alterarItem(11L, new ItemRequest("DIVERSOS", "Diversos", null, false, null), ADMIN);

        assertThat(alterado.codigo()).isEqualTo("DIVERSOS");
        assertThat(alterado.ordem()).isEqualTo(2);
    }

    @Test
    void itemComComplementoPodeContinuarAceitando() {
        InventarioItem outros = item(11, grupo(1, "LAZER", 3), "OUTROS", true, 2);
        when(itens.findById(11L)).thenReturn(Optional.of(outros));

        CatalogoItem alterado = servico.alterarItem(11L, new ItemRequest(null, "Outros", null, true, null), ADMIN);

        assertThat(alterado.permiteComplemento()).isTrue();
        verify(marcados, never()).existsByIdItemIdAndComplementoIsNotNull(anyLong());
    }

    @Test
    void itemSemTextoDosEstabelecimentosDeixaDeAceitarComplemento() {
        InventarioItem outros = item(11, grupo(1, "LAZER", 3), "OUTROS", true, 2);
        when(itens.findById(11L)).thenReturn(Optional.of(outros));
        when(marcados.existsByIdItemIdAndComplementoIsNotNull(11L)).thenReturn(false);

        CatalogoItem alterado = servico.alterarItem(11L, new ItemRequest(null, "Outros", null, false, null), ADMIN);

        assertThat(alterado.permiteComplemento()).isFalse();
    }

    @Test
    void inativarItem() {
        InventarioItem piscina = item(10, grupo(1, "LAZER", 3), "PISCINA", false, 1);
        when(itens.findById(10L)).thenReturn(Optional.of(piscina));

        servico.definirAtivoItem(10L, false, ADMIN);

        assertThat(piscina.isAtivo()).isFalse();
        verify(auditoria).registrar(1L, "inventario_item", 10L, "INATIVAR", null, null);
    }
}
