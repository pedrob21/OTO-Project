package OTO.estabelecimento;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.estabelecimento.EstabelecimentoDtos.RhcRequest;
import OTO.estabelecimento.EstabelecimentoDtos.RhcResponse;

@ExtendWith(MockitoExtension.class)
class RhcServiceTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);

    @Mock
    private RhcResumoRepository rhc;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private RhcService servico;

    @Test
    void listarDaMaisRecenteParaAMaisAntiga() {
        when(rhc.findAllByOrderByVigenteDesdeDesc()).thenReturn(List.of(
                Fixtures.rhc(2, JAN.plusMonths(6), 50, 60, 150, 3), Fixtures.rhc(1, JAN, 40, 50, 120, 3)));
        assertThat(servico.listar()).extracting(RhcResponse::id).containsExactly(2L, 1L);
    }

    @Test
    void vigenteNaData() {
        when(rhc.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JAN))
                .thenReturn(Optional.of(Fixtures.rhc(1, JAN, 40, 50, 120, 3)));
        RhcResponse vigente = servico.vigente(JAN);
        assertThat(vigente.qtdRegistros()).isEqualTo(40);
        assertThat(vigente.leitosUteis()).isEqualTo(100.0);
    }

    @Test
    void semTotaisVigentes() {
        when(rhc.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(JAN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.vigente(JAN)).isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("2026-01-01");
    }

    @Test
    void registrarNovaVigencia() {
        when(rhc.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 7L));
        RhcRequest requisicao = new RhcRequest(JAN, 40, 50, 120, 3);

        RhcResponse resposta = servico.registrar(requisicao, ADMIN);

        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.qtdLeitos()).isEqualTo(120);
        verify(auditoria).registrar(1L, "rhc_resumo", 7L, "CRIAR", null, requisicao);
    }

    @Test
    void registrarNaMesmaDataNaoPode() {
        when(rhc.existsByVigenteDesde(JAN)).thenReturn(true);
        assertThatThrownBy(() -> servico.registrar(new RhcRequest(JAN, 40, 50, 120, 3), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
        verify(rhc, never()).save(any());
    }

    @Test
    void corrigirNaMesmaDataNaoConfereRepeticao() {
        RhcResumo atual = Fixtures.rhc(1, JAN, 40, 50, 120, 3);
        when(rhc.findById(1L)).thenReturn(Optional.of(atual));

        RhcResponse depois = servico.corrigir(1L, new RhcRequest(JAN, 41, 51, 121, 3), ADMIN);

        assertThat(depois.qtdRegistros()).isEqualTo(41);
        assertThat(atual.getQtdUh()).isEqualTo(51);
        verify(rhc, never()).existsByVigenteDesde(any());
        verify(auditoria).registrar(eq(1L), eq("rhc_resumo"), eq(1L), eq("CORRIGIR"), any(), eq(depois));
    }

    @Test
    void corrigirParaDataJaUsadaNaoPode() {
        when(rhc.findById(1L)).thenReturn(Optional.of(Fixtures.rhc(1, JAN, 40, 50, 120, 3)));
        when(rhc.existsByVigenteDesde(JAN.plusDays(1))).thenReturn(true);
        assertThatThrownBy(() -> servico.corrigir(1L, new RhcRequest(JAN.plusDays(1), 40, 50, 120, 3), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void excluir() {
        RhcResumo atual = Fixtures.rhc(1, JAN, 40, 50, 120, 3);
        when(rhc.findById(1L)).thenReturn(Optional.of(atual));

        servico.excluir(1L, ADMIN);

        verify(rhc).delete(atual);
        verify(auditoria).registrar(eq(1L), eq("rhc_resumo"), eq(1L), eq("EXCLUIR"), any(), isNull());
    }

    @Test
    void corrigirOuExcluirInexistente() {
        when(rhc.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.excluir(9L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
