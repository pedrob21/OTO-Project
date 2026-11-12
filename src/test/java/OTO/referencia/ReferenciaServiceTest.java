package OTO.referencia;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.referencia.ReferenciaDtos.FaixaRef;
import OTO.referencia.ReferenciaDtos.FaixaRequest;
import OTO.referencia.ReferenciaDtos.FeriadoRef;
import OTO.referencia.ReferenciaDtos.FeriadoRequest;
import OTO.referencia.ReferenciaDtos.Referencias;
import OTO.referencia.ReferenciaDtos.TipoRef;
import OTO.referencia.ReferenciaDtos.TipoRequest;

@ExtendWith(MockitoExtension.class)
class ReferenciaServiceTest {

    @Mock
    private TipoHospedagemRepository tipos;
    @Mock
    private FeriadoEventoRepository feriados;
    @Mock
    private FaixaOcupacaoRepository faixas;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ReferenciaService servico;

    private static FaixaOcupacao faixa(String codigo, String minimo) {
        return new FaixaOcupacao(codigo, codigo.charAt(0) + codigo.substring(1).toLowerCase(), new BigDecimal(minimo));
    }

    @Test
    void listarTodasAsTabelas() {
        when(tipos.findAllByOrderByOrdem()).thenReturn(List.of(new TipoHospedagem("HOTEL", "Hotel", true, 1)));
        when(feriados.findAllByOrderByOrdem()).thenReturn(List.of(new FeriadoEvento("CARNAVAL", "Carnaval", 1)));
        when(faixas.findAllByOrderByPercentualMinimo()).thenReturn(List.of(faixa("RUIM", "0")));

        Referencias referencias = servico.listar();

        assertThat(referencias.tiposHospedagem()).containsExactly(new TipoRef("HOTEL", "Hotel", true, 1));
        assertThat(referencias.feriados()).containsExactly(new FeriadoRef("CARNAVAL", "Carnaval", 1));
        assertThat(referencias.faixasOcupacao()).containsExactly(new FaixaRef("RUIM", "Ruim", new BigDecimal("0")));
    }

    // ------------------------------------------------------------------ tipos

    @Test
    void criarTipoGeraCodigoEOrdem() {
        when(tipos.findAll()).thenReturn(List.of(new TipoHospedagem("HOTEL", "Hotel", true, 1),
                new TipoHospedagem("FLAT", "Flat", false, 5)));
        when(tipos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TipoRef criado = servico.criarTipo(new TipoRequest(null, " Chalé ", true, null), ADMIN);

        assertThat(criado).isEqualTo(new TipoRef("CHALE", "Chalé", true, 6));
        verify(auditoria).registrar(1L, "tipo_hospedagem", "CHALE", "CRIAR", null, criado);
    }

    @Test
    void criarTipoComCodigoRepetido() {
        when(tipos.existsById("HOTEL")).thenReturn(true);
        assertThatThrownBy(() -> servico.criarTipo(new TipoRequest("hotel", "Hotel", true, 1), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um tipo de hospedagem com o código HOTEL.");
    }

    @Test
    void alterarTipoMantemOCodigo() {
        TipoHospedagem hotel = new TipoHospedagem("HOTEL", "Hotel", true, 1);
        when(tipos.findById("HOTEL")).thenReturn(Optional.of(hotel));

        TipoRef depois = servico.alterarTipo("HOTEL", new TipoRequest("hotel", "Hotéis", false, null), ADMIN);

        assertThat(depois).isEqualTo(new TipoRef("HOTEL", "Hotéis", false, 1));
        verify(auditoria).registrar(eq(1L), eq("tipo_hospedagem"), eq("HOTEL"), eq("ATUALIZAR"), any(), eq(depois));
    }

    @Test
    void codigoNaoPodeMudar() {
        when(tipos.findById("HOTEL")).thenReturn(Optional.of(new TipoHospedagem("HOTEL", "Hotel", true, 1)));
        assertThatThrownBy(() -> servico.alterarTipo("HOTEL", new TipoRequest("HOTEIS", "Hotel", true, 1), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("código não pode ser alterado");
    }

    @Test
    void excluirTipoUsadoNaoPode() {
        TipoHospedagem hotel = new TipoHospedagem("HOTEL", "Hotel", true, 1);
        when(tipos.findById("HOTEL")).thenReturn(Optional.of(hotel));
        doThrow(new DataIntegrityViolationException("fk")).when(tipos).flush();

        assertThatThrownBy(() -> servico.excluirTipo("HOTEL", ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("O tipo Hotel já é usado");
        verify(auditoria, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void excluirTipoNuncaUsado() {
        TipoHospedagem chale = new TipoHospedagem("CHALE", "Chalé", true, 6);
        when(tipos.findById("CHALE")).thenReturn(Optional.of(chale));

        servico.excluirTipo("CHALE", ADMIN);

        verify(tipos).delete(chale);
        verify(auditoria).registrar(eq(1L), eq("tipo_hospedagem"), eq("CHALE"), eq("EXCLUIR"), any(), isNull());
    }

    @Test
    void tipoInexistente() {
        when(tipos.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.excluirTipo("X", ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ------------------------------------------------------------------ feriados

    @Test
    void criarFeriadoComCodigoInformadoEOrdem() {
        when(feriados.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeriadoRef criado = servico.criarFeriado(new FeriadoRequest("festival_folclore", "Festival do Folclore", 3), ADMIN);

        assertThat(criado).isEqualTo(new FeriadoRef("FESTIVAL_FOLCLORE", "Festival do Folclore", 3));
        verify(feriados, never()).findAll();
    }

    @Test
    void criarFeriadoSemCodigoNemOrdem() {
        when(feriados.existsById(anyString())).thenAnswer(inv -> "PASCOA".equals(inv.getArgument(0)));
        when(feriados.findAll()).thenReturn(List.of());
        when(feriados.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeriadoRef criado = servico.criarFeriado(new FeriadoRequest(" ", "Páscoa", null), ADMIN);

        assertThat(criado).isEqualTo(new FeriadoRef("PASCOA_2", "Páscoa", 1));
    }

    @Test
    void alterarEExcluirFeriado() {
        FeriadoEvento carnaval = new FeriadoEvento("CARNAVAL", "Carnaval", 1);
        when(feriados.findById("CARNAVAL")).thenReturn(Optional.of(carnaval));

        assertThat(servico.alterarFeriado("CARNAVAL", new FeriadoRequest(null, "Carnaval de rua", 2), ADMIN))
                .isEqualTo(new FeriadoRef("CARNAVAL", "Carnaval de rua", 2));

        doThrow(new DataIntegrityViolationException("fk")).when(feriados).flush();
        assertThatThrownBy(() -> servico.excluirFeriado("CARNAVAL", ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já é usado");
    }

    @Test
    void excluirFeriadoNuncaUsado() {
        FeriadoEvento festa = new FeriadoEvento("FESTA", "Festa", 9);
        when(feriados.findById("FESTA")).thenReturn(Optional.of(festa));
        servico.excluirFeriado("FESTA", ADMIN);
        verify(feriados).delete(festa);
    }

    @Test
    void feriadoInexistente() {
        when(feriados.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.alterarFeriado("X", new FeriadoRequest(null, "X", 1), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ------------------------------------------------------------------ faixas

    @Test
    void criarFaixaComDuasCasas() {
        when(faixas.findAll()).thenReturn(List.of(faixa("RUIM", "0.00"), faixa("BOA", "41.00")));
        when(faixas.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FaixaRef criada = servico.criarFaixa(new FaixaRequest(null, "Muito boa", new BigDecimal("51.5")), ADMIN);

        assertThat(criada).isEqualTo(new FaixaRef("MUITO_BOA", "Muito boa", new BigDecimal("51.50")));
    }

    @Test
    void duasFaixasNaoComecamNoMesmoPercentual() {
        when(faixas.findAll()).thenReturn(List.of(faixa("RUIM", "0.00"), faixa("BOA", "41.50")));
        assertThatThrownBy(() -> servico.criarFaixa(new FaixaRequest("NOVA", "Nova", new BigDecimal("41.5")), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A faixa Boa já começa em 41,5%.");
    }

    @Test
    void alterarFaixaPodeManterOProprioPercentual() {
        FaixaOcupacao boa = faixa("BOA", "41.00");
        when(faixas.findById("BOA")).thenReturn(Optional.of(boa));
        when(faixas.findAll()).thenReturn(List.of(faixa("RUIM", "0.00"), boa));

        FaixaRef depois = servico.alterarFaixa("BOA", new FaixaRequest(null, "Boa ocupação", new BigDecimal("41")), ADMIN);

        assertThat(depois).isEqualTo(new FaixaRef("BOA", "Boa ocupação", new BigDecimal("41.00")));
    }

    @Test
    void aPrimeiraFaixaContinuaEmZero() {
        when(faixas.findById("RUIM")).thenReturn(Optional.of(faixa("RUIM", "0.00")));
        assertThatThrownBy(() -> servico.alterarFaixa("RUIM", new FaixaRequest(null, "Ruim", new BigDecimal("5")), ADMIN))
                .hasMessageContaining("começa em 0%");
        assertThatThrownBy(() -> servico.excluirFaixa("RUIM", ADMIN)).hasMessageContaining("não pode ser excluída");
        verify(faixas, never()).delete(any());
    }

    @Test
    void excluirFaixa() {
        FaixaOcupacao boa = faixa("BOA", "41.00");
        when(faixas.findById("BOA")).thenReturn(Optional.of(boa));

        servico.excluirFaixa("BOA", ADMIN);
        verify(faixas).delete(boa);

        doThrow(new DataIntegrityViolationException("fk")).when(faixas).flush();
        assertThatThrownBy(() -> servico.excluirFaixa("BOA", ADMIN)).hasMessageContaining("já foi usada");
    }

    @Test
    void faixaInexistente() {
        when(faixas.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.excluirFaixa("X", ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
