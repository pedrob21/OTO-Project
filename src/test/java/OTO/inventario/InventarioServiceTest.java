package OTO.inventario;

import static OTO.Fixtures.ADMIN;
import static OTO.Fixtures.PARCEIRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.ControleAcesso;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.inventario.InventarioDtos.CatalogoGrupo;
import OTO.inventario.InventarioDtos.CatalogoItem;
import OTO.inventario.InventarioDtos.InventarioRequest;
import OTO.inventario.InventarioDtos.InventarioResponse;
import OTO.inventario.InventarioDtos.ItemMarcado;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioItemRepository itens;
    @Mock
    private EstabelecimentoInventarioItemRepository marcados;
    @Mock
    private EstabelecimentoRepository estabelecimentos;
    @Mock
    private ControleAcesso acesso;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private InventarioService servico;

    private final InventarioGrupo lazer = Fixtures.comId(
            new InventarioGrupo(CategoriaInventario.SERVICOS_EQUIPAMENTOS, "LAZER", "Lazer", false, 1), 1L);
    private final InventarioGrupo ods = Fixtures.comId(
            new InventarioGrupo(CategoriaInventario.SUSTENTABILIDADE, "ODS", "Indicadores ODS", true, 2), 2L);
    private final InventarioItem piscina = Fixtures.comId(new InventarioItem(lazer, "PISCINA", "Piscina", null, false, 1), 10L);
    private final InventarioItem outros = Fixtures.comId(new InventarioItem(lazer, "OUTROS", "Outros", null, true, 2), 11L);
    private final InventarioItem energia = Fixtures.comId(new InventarioItem(ods, "ENERGIA", "Energia solar", null, false, 1), 20L);
    private final Estabelecimento hotel = Fixtures.estabelecimento(5, "Hotel A", "HOTEL");

    @BeforeEach
    void catalogo() {
        org.mockito.Mockito.lenient().when(itens.findCatalogoAtivo()).thenReturn(List.of(piscina, outros, energia));
        org.mockito.Mockito.lenient().when(estabelecimentos.findById(5L)).thenReturn(Optional.of(hotel));
    }

    private static EstabelecimentoInventarioItem marcado(long itemId, String complemento) {
        return new EstabelecimentoInventarioItem(5L, itemId, complemento, 1L);
    }

    private static InventarioRequest pedido(ItemMarcado... itens) {
        return new InventarioRequest(List.of(itens));
    }

    @Test
    void catalogoAgrupaOsItensNaOrdem() {
        List<CatalogoGrupo> grupos = servico.catalogo();

        assertThat(grupos).extracting(CatalogoGrupo::codigo).containsExactly("LAZER", "ODS");
        assertThat(grupos.get(0).itens()).extracting(CatalogoItem::codigo).containsExactly("PISCINA", "OUTROS");
        assertThat(grupos.get(1).exclusivoSecretaria()).isTrue();
    }

    @Test
    void buscarConfereOAcessoEOrdenaPeloItem() {
        when(marcados.findByIdEstabelecimentoId(5L)).thenReturn(List.of(marcado(11, "Quadra"), marcado(10, null)));

        InventarioResponse resposta = servico.buscar(5L, PARCEIRO);

        verify(acesso).exigirLeitura(PARCEIRO, 5L);
        assertThat(resposta.estabelecimentoId()).isEqualTo(5L);
        assertThat(resposta.itens()).containsExactly(new ItemMarcado(10L, null), new ItemMarcado(11L, "Quadra"));
    }

    @Test
    void buscarEstabelecimentoInexistente() {
        when(estabelecimentos.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.buscar(9L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void salvarSemPermissaoNaoMexeEmNada() {
        doThrow(new AccessDeniedException("não")).when(acesso).exigirEscrita(PARCEIRO, 5L);
        assertThatThrownBy(() -> servico.salvar(5L, pedido(), PARCEIRO)).isInstanceOf(AccessDeniedException.class);
        verify(marcados, never()).save(any());
    }

    @Test
    void salvarSubstituiOsItensMarcados() {
        EstabelecimentoInventarioItem piscinaMarcada = marcado(10, null);
        EstabelecimentoInventarioItem outrosMarcado = marcado(11, "Quadra");
        when(marcados.findByIdEstabelecimentoId(5L))
                .thenReturn(List.of(piscinaMarcada, outrosMarcado))
                .thenReturn(List.of(outrosMarcado, marcado(20, null)));

        InventarioResponse resposta = servico.salvar(5L,
                pedido(new ItemMarcado(11L, "  Quadra de tênis "), new ItemMarcado(20L, "  ")), ADMIN);

        verify(marcados).delete(piscinaMarcada);
        assertThat(outrosMarcado.getComplemento()).isEqualTo("Quadra de tênis");
        ArgumentCaptor<EstabelecimentoInventarioItem> novo = ArgumentCaptor.forClass(EstabelecimentoInventarioItem.class);
        verify(marcados).save(novo.capture());
        assertThat(novo.getValue().getItemId()).isEqualTo(20L);
        assertThat(novo.getValue().getComplemento()).isNull();
        assertThat(hotel.getInventarioAtualizadoEm()).isNotNull();
        assertThat(resposta.itens()).extracting(ItemMarcado::itemId).containsExactly(11L, 20L);
        verify(auditoria).registrar(eq(1L), eq("estabelecimento_inventario"), eq(5L), eq("ATUALIZAR"),
                eq(Map.of("itens", List.of(new ItemMarcado(10L, null), new ItemMarcado(11L, "Quadra")))), any());
    }

    @Test
    void parceiroNaoMexeNosGruposExclusivosDaSecretaria() {
        EstabelecimentoInventarioItem energiaMarcada = marcado(20, null);
        when(marcados.findByIdEstabelecimentoId(5L)).thenReturn(List.of(energiaMarcada));

        servico.salvar(5L, pedido(new ItemMarcado(10L, null)), PARCEIRO);

        verify(marcados, never()).delete(any());
        ArgumentCaptor<EstabelecimentoInventarioItem> novo = ArgumentCaptor.forClass(EstabelecimentoInventarioItem.class);
        verify(marcados).save(novo.capture());
        assertThat(novo.getValue().getItemId()).isEqualTo(10L);
    }

    @Test
    void parceiroNaoMarcaItemExclusivoDaSecretaria() {
        when(marcados.findByIdEstabelecimentoId(5L)).thenReturn(List.of());
        servico.salvar(5L, pedido(new ItemMarcado(20L, null)), PARCEIRO);
        verify(marcados, never()).save(any());
    }

    @Test
    void itemInativoJaMarcadoFicaComoEsta() {
        EstabelecimentoInventarioItem inativo = marcado(99, "antigo");
        when(marcados.findByIdEstabelecimentoId(5L)).thenReturn(List.of(inativo));

        servico.salvar(5L, pedido(), ADMIN);

        verify(marcados, never()).delete(any());
        assertThat(inativo.getComplemento()).isEqualTo("antigo");
    }

    @Test
    void recusaItemInexistenteRepetidoOuComComplementoIndevido() {
        assertThatThrownBy(() -> servico.salvar(5L, pedido(new ItemMarcado(99L, null)), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inexistente ou inativo: 99");
        assertThatThrownBy(() -> servico.salvar(5L, pedido(new ItemMarcado(10L, null), new ItemMarcado(10L, null)), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("mais de uma vez");
        assertThatThrownBy(() -> servico.salvar(5L, pedido(new ItemMarcado(10L, "aquecida")), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não aceita complemento");
        verify(marcados, never()).save(any());
    }
}
