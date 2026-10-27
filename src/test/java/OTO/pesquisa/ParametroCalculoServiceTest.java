package OTO.pesquisa;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.calculo.ResultadoCampanhaRepository;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.pesquisa.ParametroCalculoDtos.ParametroRequest;
import OTO.pesquisa.ParametroCalculoDtos.ParametroResponse;

@ExtendWith(MockitoExtension.class)
class ParametroCalculoServiceTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);

    @Mock
    private ParametroCalculoRepository parametros;
    @Mock
    private ResultadoCampanhaRepository resultados;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ParametroCalculoService servico;

    private static ParametroRequest requisicao(LocalDate desde, int permanencia, int parques) {
        return new ParametroRequest(desde, new BigDecimal("15.15"), permanencia, parques, new BigDecimal("250.00"),
                new BigDecimal("105.00"), new BigDecimal("30.00"), "  Pesquisa de demanda 2026 ");
    }

    @Test
    void listarEVigente() {
        when(parametros.findAllByOrderByVigenteDesdeDesc()).thenReturn(List.of(Fixtures.parametro(2, JAN.plusYears(1)),
                Fixtures.parametro(1, JAN)));
        assertThat(servico.listar()).extracting(ParametroResponse::id).containsExactly(2L, 1L);

        when(parametros.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JAN))
                .thenReturn(Optional.of(Fixtures.parametro(1, JAN)));
        ParametroResponse vigente = servico.vigente(JAN);
        assertThat(vigente.percentualDayUse()).isEqualByComparingTo("15.15");
        assertThat(vigente.diasPermanenciaMedia()).isEqualTo(4);
    }

    @Test
    void semParametrosVigentes() {
        when(parametros.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JAN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.vigente(JAN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void registrarNovaVigencia() {
        when(parametros.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 5L));
        ParametroRequest r = requisicao(JAN, 4, 2);

        ParametroResponse criado = servico.registrar(r, ADMIN);

        assertThat(criado.id()).isEqualTo(5L);
        assertThat(criado.fonte()).isEqualTo("Pesquisa de demanda 2026");
        verify(auditoria).registrar(1L, "parametro_calculo", 5L, "CRIAR", null, r);
    }

    @Test
    void registrarRecusaParquesAcimaDaPermanenciaEDataRepetida() {
        assertThatThrownBy(() -> servico.registrar(requisicao(JAN, 2, 3), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("dias em parques");

        when(parametros.existsByVigenteDesde(JAN)).thenReturn(true);
        assertThatThrownBy(() -> servico.registrar(requisicao(JAN, 4, 2), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("outra data");
        verify(parametros, never()).save(any());
    }

    @Test
    void alterarVigenciaNaoUsada() {
        ParametroCalculo parametro = Fixtures.parametro(1, JAN);
        when(parametros.findById(1L)).thenReturn(Optional.of(parametro));

        ParametroResponse depois = servico.alterar(1L, requisicao(JAN, 5, 2), ADMIN);

        assertThat(depois.diasPermanenciaMedia()).isEqualTo(5);
        assertThat(parametro.getDiasPermanenciaMedia()).isEqualTo(5);
        verify(parametros, never()).existsByVigenteDesde(any());
        verify(auditoria).registrar(eq(1L), eq("parametro_calculo"), eq(1L), eq("ATUALIZAR"), any(), eq(depois));
    }

    @Test
    void alterarRecusaVigenciaUsadaParquesEDataDeOutra() {
        when(parametros.findById(1L)).thenReturn(Optional.of(Fixtures.parametro(1, JAN)));

        when(resultados.existsByParametroCalculoId(1L)).thenReturn(true);
        assertThatThrownBy(() -> servico.alterar(1L, requisicao(JAN, 4, 2), ADMIN))
                .hasMessageContaining("já foram usados em resultados publicados");

        when(resultados.existsByParametroCalculoId(1L)).thenReturn(false);
        assertThatThrownBy(() -> servico.alterar(1L, requisicao(JAN, 2, 3), ADMIN))
                .hasMessageContaining("dias em parques");

        when(parametros.existsByVigenteDesde(JAN.plusMonths(1))).thenReturn(true);
        assertThatThrownBy(() -> servico.alterar(1L, requisicao(JAN.plusMonths(1), 4, 2), ADMIN))
                .hasMessageContaining("outra data");
    }

    @Test
    void alterarOuExcluirInexistente() {
        when(parametros.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.alterar(9L, requisicao(JAN, 4, 2), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.excluir(9L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void excluirVigenciaNaoUsada() {
        ParametroCalculo parametro = Fixtures.parametro(1, JAN);
        when(parametros.findById(1L)).thenReturn(Optional.of(parametro));
        when(parametros.count()).thenReturn(2L);

        servico.excluir(1L, ADMIN);

        verify(parametros).delete(parametro);
        verify(auditoria).registrar(eq(1L), eq("parametro_calculo"), eq(1L), eq("EXCLUIR"), any(), isNull());
    }

    @Test
    void naoExcluiVigenciaUsadaNemAUnica() {
        when(parametros.findById(1L)).thenReturn(Optional.of(Fixtures.parametro(1, JAN)));

        when(resultados.existsByParametroCalculoId(1L)).thenReturn(true);
        assertThatThrownBy(() -> servico.excluir(1L, ADMIN)).hasMessageContaining("não podem ser excluídos");

        when(resultados.existsByParametroCalculoId(1L)).thenReturn(false);
        when(parametros.count()).thenReturn(1L);
        assertThatThrownBy(() -> servico.excluir(1L, ADMIN)).hasMessageContaining("única vigência");
        verify(parametros, never()).delete(any());
    }
}
