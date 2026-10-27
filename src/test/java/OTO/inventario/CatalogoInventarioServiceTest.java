package OTO.inventario;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.springframework.data.domain.Sort;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.CatalogoItem;
import OTO.inventario.InventarioDtos.GrupoRequest;
import OTO.inventario.InventarioDtos.ItemRequest;

@ExtendWith(MockitoExtension.class)
class CatalogoInventarioServiceTest {

    private static final String SEGMENTO = InventarioGrupo.SEGMENTO_MEIO_HOSPEDAGEM;

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

    // ------------------------------------------------------------------ catálogo completo

    @Test
    void completoTrazGruposSemItensEInativos() {
        InventarioGrupo lazer = grupo(1, "LAZER", 1);
        InventarioGrupo vazio = grupo(2, "VAZIO", 2);
        vazio.definirAtivo(false);
        InventarioItem piscina = item(10, lazer, "PISCINA", false, 1);
        piscina.definirAtivo(false);
        when(grupos.findAll(any(Sort.class))).thenReturn(List.of(lazer, vazio));
        when(itens.findAll(any(Sort.class))).thenReturn(List.of(piscina));

        List<CatalogoGrupo> catalogo = servico.completo();

        assertThat(catalogo).extracting(CatalogoGrupo::codigo).containsExactly("LAZER", "VAZIO");
        assertThat(catalogo.get(0).itens()).extracting(CatalogoItem::ativo).containsExactly(false);
        assertThat(catalogo.get(1).itens()).isEmpty();
        assertThat(catalogo.get(1).ativo()).isFalse();
    }

    // ------------------------------------------------------------------ grupos

    @Test
    void criarGrupoGeraOCodigoEVaiParaOFim() {
        when(grupos.existsBySegmentoAndCodigo(eq(SEGMENTO), anyString()))
                .thenAnswer(inv -> "AREA_DE_LAZER".equals(inv.getArgument(1)));
        when(grupos.findAll()).thenReturn(List.of(grupo(1, "A", 3), grupo(2, "B", 7)));
        when(grupos.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 9L));

        CatalogoGrupo criado = servico.criarGrupo(new GrupoRequest(CategoriaInventario.ACESSIBILIDADE, null,
                " Área de lazer ", true, null), ADMIN);

        assertThat(criado.id()).isEqualTo(9L);
        assertThat(criado.codigo()).isEqualTo("AREA_DE_LAZER_2");
        assertThat(criado.nome()).isEqualTo("Área de lazer");
        assertThat(criado.ordem()).isEqualTo(8);
        assertThat(criado.exclusivoSecretaria()).isTrue();
        verify(auditoria).registrar(1L, "inventario_grupo", 9L, "CRIAR", null, criado);
    }

    @Test
    void criarGrupoComCodigoEOrdemInformados() {
        when(grupos.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 9L));

        CatalogoGrupo criado = servico.criarGrupo(new GrupoRequest(CategoriaInventario.ACESSIBILIDADE, "rampa",
                "Rampa", false, 4), ADMIN);

        assertThat(criado.codigo()).isEqualTo("RAMPA");
        assertThat(criado.ordem()).isEqualTo(4);
        verify(grupos, never()).findAll();
    }

    @Test
    void criarGrupoComCodigoRepetido() {
        when(grupos.existsBySegmentoAndCodigo(SEGMENTO, "RAMPA")).thenReturn(true);
        assertThatThrownBy(() -> servico.criarGrupo(new GrupoRequest(CategoriaInventario.ACESSIBILIDADE, "RAMPA",
                "Rampa", false, 1), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um grupo com o código RAMPA.");
    }

    @Test
    void alterarGrupoMantemCodigoEOrdemQuandoNaoInformados() {
        InventarioGrupo lazer = grupo(1, "LAZER", 3);
        when(grupos.findById(1L)).thenReturn(Optional.of(lazer));
        when(itens.findByGrupoIdOrderByOrdemAscIdAsc(1L)).thenReturn(List.of(item(10, lazer, "PISCINA", false, 1)));

        CatalogoGrupo alterado = servico.alterarGrupo(1L, new GrupoRequest(CategoriaInventario.SUSTENTABILIDADE, " ",
                "Lazer e recreação", false, null), ADMIN);

        assertThat(alterado.codigo()).isEqualTo("LAZER");
        assertThat(alterado.ordem()).isEqualTo(3);
        assertThat(alterado.categoria()).isEqualTo(CategoriaInventario.SUSTENTABILIDADE);
        assertThat(alterado.itens()).hasSize(1);
        verify(grupos, never()).existsBySegmentoAndCodigoAndIdNot(anyString(), anyString(), anyLong());
        verify(auditoria).registrar(eq(1L), eq("inventario_grupo"), eq(1L), eq("ATUALIZAR"), any(), any());
    }

    @Test
    void alterarGrupoParaCodigoDeOutro() {
        when(grupos.findById(1L)).thenReturn(Optional.of(grupo(1, "LAZER", 3)));
        when(grupos.existsBySegmentoAndCodigoAndIdNot(SEGMENTO, "RAMPA", 1L)).thenReturn(true);
        assertThatThrownBy(() -> servico.alterarGrupo(1L, new GrupoRequest(CategoriaInventario.ACESSIBILIDADE, "rampa",
                "Lazer", false, null), ADMIN)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void inativarGrupo() {
        InventarioGrupo lazer = grupo(1, "LAZER", 3);
        when(grupos.findById(1L)).thenReturn(Optional.of(lazer));
        servico.definirAtivoGrupo(1L, false, ADMIN);
        assertThat(lazer.isAtivo()).isFalse();
        verify(auditoria).registrar(1L, "inventario_grupo", 1L, "INATIVAR", null, null);
    }

    @Test
    void excluirGrupoSoSemItens() {
        InventarioGrupo lazer = grupo(1, "LAZER", 3);
        when(grupos.findById(1L)).thenReturn(Optional.of(lazer));
        when(itens.existsByGrupoId(1L)).thenReturn(true).thenReturn(false);

        assertThatThrownBy(() -> servico.excluirGrupo(1L, ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("tem itens");
        verify(grupos, never()).delete(any());

        servico.excluirGrupo(1L, ADMIN);
        verify(grupos).delete(lazer);
        verify(auditoria).registrar(eq(1L), eq("inventario_grupo"), eq(1L), eq("EXCLUIR"), any(), isNull());
    }

    @Test
    void grupoInexistente() {
        when(grupos.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.definirAtivoGrupo(9L, true, ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ------------------------------------------------------------------ itens

    @Test
    void criarItemGeraOCodigoNoGrupoEVaiParaOFim() {
        InventarioGrupo lazer = grupo(1, "LAZER", 3);
        when(grupos.findById(1L)).thenReturn(Optional.of(lazer));
        when(itens.findByGrupoIdOrderByOrdemAscIdAsc(1L)).thenReturn(List.of(item(10, lazer, "PISCINA", false, 5)));
        when(itens.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 11L));

        CatalogoItem criado = servico.criarItem(1L, new ItemRequest(null, "Sauna seca", "  ", false, null), ADMIN);

        assertThat(criado.id()).isEqualTo(11L);
        assertThat(criado.codigo()).isEqualTo("SAUNA_SECA");
        assertThat(criado.descricao()).isNull();
        assertThat(criado.ordem()).isEqualTo(6);
        verify(itens).existsByGrupoIdAndCodigo(1L, "SAUNA_SECA");
        verify(auditoria).registrar(1L, "inventario_item", 11L, "CRIAR", null, criado);
    }

    @Test
    void criarItemComCodigoRepetidoNoGrupo() {
        when(grupos.findById(1L)).thenReturn(Optional.of(grupo(1, "LAZER", 3)));
        when(itens.existsByGrupoIdAndCodigo(1L, "SAUNA")).thenReturn(true);
        assertThatThrownBy(() -> servico.criarItem(1L, new ItemRequest("sauna", "Sauna", null, false, 1), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("neste grupo");
    }

    @Test
    void alterarItem() {
        InventarioItem outros = item(11, grupo(1, "LAZER", 3), "OUTROS", true, 2);
        when(itens.findById(11L)).thenReturn(Optional.of(outros));

        CatalogoItem alterado = servico.alterarItem(11L, new ItemRequest(null, " Outros serviços ", " Descreva ", true, 9),
                ADMIN);

        assertThat(alterado.codigo()).isEqualTo("OUTROS");
        assertThat(alterado.nome()).isEqualTo("Outros serviços");
        assertThat(alterado.descricao()).isEqualTo("Descreva");
        assertThat(alterado.ordem()).isEqualTo(9);
        verify(auditoria).registrar(eq(1L), eq("inventario_item"), eq(11L), eq("ATUALIZAR"), any(), eq(alterado));
    }

    @Test
    void itemComTextoDosEstabelecimentosContinuaAceitandoComplemento() {
        when(itens.findById(11L)).thenReturn(Optional.of(item(11, grupo(1, "LAZER", 3), "OUTROS", true, 2)));
        when(marcados.existsByIdItemIdAndComplementoIsNotNull(11L)).thenReturn(true);

        assertThatThrownBy(() -> servico.alterarItem(11L, new ItemRequest(null, "Outros", null, false, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("complemento");
    }

    @Test
    void alterarItemParaCodigoDeOutroDoGrupo() {
        when(itens.findById(11L)).thenReturn(Optional.of(item(11, grupo(1, "LAZER", 3), "OUTROS", false, 2)));
        when(itens.existsByGrupoIdAndCodigoAndIdNot(1L, "SAUNA", 11L)).thenReturn(true);
        assertThatThrownBy(() -> servico.alterarItem(11L, new ItemRequest("SAUNA", "Outros", null, false, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void reativarItem() {
        InventarioItem piscina = item(10, grupo(1, "LAZER", 3), "PISCINA", false, 1);
        piscina.definirAtivo(false);
        when(itens.findById(10L)).thenReturn(Optional.of(piscina));
        servico.definirAtivoItem(10L, true, ADMIN);
        assertThat(piscina.isAtivo()).isTrue();
        verify(auditoria).registrar(1L, "inventario_item", 10L, "REATIVAR", null, null);
    }

    @Test
    void excluirItemSoSeNinguemMarcou() {
        InventarioItem piscina = item(10, grupo(1, "LAZER", 3), "PISCINA", false, 1);
        when(itens.findById(10L)).thenReturn(Optional.of(piscina));
        when(marcados.existsByIdItemId(10L)).thenReturn(true).thenReturn(false);

        assertThatThrownBy(() -> servico.excluirItem(10L, ADMIN)).isInstanceOf(RegraNegocioException.class);
        verify(itens, never()).delete(any());

        servico.excluirItem(10L, ADMIN);
        verify(itens).delete(piscina);
    }

    @Test
    void itemInexistente() {
        when(itens.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.excluirItem(9L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Item do inventário 9 não encontrado(a).");
    }
}
