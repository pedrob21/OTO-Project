package OTO.referencia;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.referencia.ReferenciaDtos.FaixaRef;
import OTO.referencia.ReferenciaDtos.FaixaRequest;
import OTO.referencia.ReferenciaDtos.FeriadoRef;
import OTO.referencia.ReferenciaDtos.FeriadoRequest;
import OTO.referencia.ReferenciaDtos.TipoRef;
import OTO.referencia.ReferenciaDtos.TipoRequest;

/** Ordem informada ou mantida, registros inexistentes e a primeira faixa renomeada. */
@ExtendWith(MockitoExtension.class)
class ReferenciaServiceComplementarTest {

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

    @Test
    void criarTipoComOrdemInformada() {
        when(tipos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TipoRef criado = servico.criarTipo(new TipoRequest("CHALE", "Chalé", false, 4), ADMIN);

        assertThat(criado).isEqualTo(new TipoRef("CHALE", "Chalé", false, 4));
        verify(tipos, never()).findAll();
    }

    @Test
    void alterarTipoComESemOrdem() {
        when(tipos.findById("HOTEL")).thenReturn(Optional.of(new TipoHospedagem("HOTEL", "Hotel", true, 3)));

        assertThat(servico.alterarTipo("HOTEL", new TipoRequest(null, "Hotel urbano", true, null), ADMIN))
                .isEqualTo(new TipoRef("HOTEL", "Hotel urbano", true, 3));
        assertThat(servico.alterarTipo("HOTEL", new TipoRequest("hotel", "Hotel", false, 7), ADMIN))
                .isEqualTo(new TipoRef("HOTEL", "Hotel", false, 7));
    }

    @Test
    void alterarTipoInexistente() {
        when(tipos.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.alterarTipo("X", new TipoRequest(null, "X", false, null), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void alterarFeriadoComESemOrdem() {
        when(feriados.findById("CARNAVAL")).thenReturn(Optional.of(new FeriadoEvento("CARNAVAL", "Carnaval", 2)));

        assertThat(servico.alterarFeriado("CARNAVAL", new FeriadoRequest(null, "Carnaval de rua", null), ADMIN))
                .isEqualTo(new FeriadoRef("CARNAVAL", "Carnaval de rua", 2));
        assertThat(servico.alterarFeriado("CARNAVAL", new FeriadoRequest(null, "Carnaval", 5), ADMIN))
                .isEqualTo(new FeriadoRef("CARNAVAL", "Carnaval", 5));
    }

    @Test
    void excluirFeriadoInexistente() {
        when(feriados.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.excluirFeriado("X", ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void alterarFaixaInexistente() {
        when(faixas.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.alterarFaixa("X", new FaixaRequest(null, "X", BigDecimal.TEN), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void primeiraFaixaPodeMudarSoONome() {
        FaixaOcupacao ruim = new FaixaOcupacao("RUIM", "Ruim", BigDecimal.ZERO);
        when(faixas.findById("RUIM")).thenReturn(Optional.of(ruim));
        when(faixas.findAll()).thenReturn(List.of(ruim));

        FaixaRef alterada = servico.alterarFaixa("RUIM", new FaixaRequest(null, "Muito ruim", BigDecimal.ZERO), ADMIN);

        assertThat(alterada.nome()).isEqualTo("Muito ruim");
        assertThat(alterada.percentualMinimo()).isEqualByComparingTo("0");
    }

    @Test
    void construtoresDoJpa() {
        assertThat(new TipoHospedagem().getCodigo()).isNull();
        assertThat(new FeriadoEvento().getCodigo()).isNull();
        assertThat(new FaixaOcupacao().getCodigo()).isNull();
    }
}
