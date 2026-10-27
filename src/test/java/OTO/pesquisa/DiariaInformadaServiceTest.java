package OTO.pesquisa;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.pesquisa.DiariaInformadaDtos.DiariaInformada;
import OTO.pesquisa.DiariaInformadaDtos.DiariaInformadaResponse;
import OTO.pesquisa.DiariaInformadaDtos.DiariasRequest;
import OTO.referencia.TipoHospedagemRepository;

@ExtendWith(MockitoExtension.class)
class DiariaInformadaServiceTest {

    @Mock
    private CampanhaRepository campanhas;
    @Mock
    private CampanhaDiariaRepository diarias;
    @Mock
    private TipoHospedagemRepository tipos;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private DiariaInformadaService servico;

    private Campanha campanha() {
        Campanha c = Fixtures.campanhaMensal(10, Fixtures.rodada(1, "005/26", StatusRodada.ENCERRADA), YearMonth.of(2026, 2));
        when(campanhas.findById(10L)).thenReturn(Optional.of(c));
        return c;
    }

    private static CampanhaDiaria diaria(String tipo, String valor) {
        return new CampanhaDiaria(10L, tipo, new BigDecimal(valor), "Booking", 1L);
    }

    private static DiariasRequest pedido(String fonte, DiariaInformada... itens) {
        return new DiariasRequest(Arrays.asList(itens), fonte);
    }

    private static DiariaInformada de(String tipo, String valor) {
        return new DiariaInformada(tipo, new BigDecimal(valor));
    }

    @Test
    void listarComAGeralPrimeiro() {
        campanha();
        when(diarias.findByCampanhaId(10L)).thenReturn(List.of(diaria("POUSADA", "300"), diaria(null, "420"),
                diaria("HOTEL", "450")));

        assertThat(servico.listar(10L)).extracting(DiariaInformadaResponse::tipoHospedagemCodigo)
                .containsExactly(null, "HOTEL", "POUSADA");
    }

    @Test
    void campanhaInexistente() {
        when(campanhas.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.listar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void salvarSubstituiAsDiariasDigitadas() {
        campanha();
        CampanhaDiaria geral = diaria(null, "400");
        CampanhaDiaria hotel = diaria("HOTEL", "450");
        CampanhaDiaria pousada = diaria("POUSADA", "300");
        when(diarias.findByCampanhaId(10L)).thenReturn(List.of(geral, hotel, pousada))
                .thenReturn(List.of(geral, hotel));
        when(tipos.existsById(anyString())).thenReturn(true);

        servico.salvar(10L, pedido(" Booking março ", de("  ", "420"), de(" HOTEL ", "460"), de("FLAT", "380")), ADMIN);

        assertThat(geral.getDiariaMedia()).isEqualByComparingTo("420");
        assertThat(geral.getFonte()).isEqualTo("Booking março");
        assertThat(hotel.getDiariaMedia()).isEqualByComparingTo("460");
        verify(diarias).delete(pousada);
        ArgumentCaptor<CampanhaDiaria> nova = ArgumentCaptor.forClass(CampanhaDiaria.class);
        verify(diarias).save(nova.capture());
        assertThat(nova.getValue().getTipoHospedagemCodigo()).isEqualTo("FLAT");
        assertThat(nova.getValue().getDiariaMedia()).isEqualByComparingTo("380");
        verify(auditoria).registrar(eq(1L), eq("campanha_diaria"), eq(10L), eq("ATUALIZAR"), any(Map.class), any(Map.class));
    }

    @Test
    void listaVaziaApagaTodasEFonteEmBrancoFicaNula() {
        campanha();
        CampanhaDiaria geral = diaria(null, "400");
        when(diarias.findByCampanhaId(10L)).thenReturn(List.of(geral)).thenReturn(List.of());

        assertThat(servico.salvar(10L, pedido("  "), ADMIN)).isEmpty();
        verify(diarias).delete(geral);

        when(diarias.findByCampanhaId(10L)).thenReturn(List.of());
        servico.salvar(10L, pedido(null, de(null, "410")), ADMIN);
        ArgumentCaptor<CampanhaDiaria> nova = ArgumentCaptor.forClass(CampanhaDiaria.class);
        verify(diarias).save(nova.capture());
        assertThat(nova.getValue().isGeral()).isTrue();
        assertThat(nova.getValue().getFonte()).isNull();
    }

    @Test
    void campanhaPublicadaNaoMuda() {
        campanha().publicar(1L);
        assertThatThrownBy(() -> servico.salvar(10L, pedido(null), ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("publicada");
    }

    @Test
    void recusaTipoInvalidoERepetido() {
        campanha();
        when(tipos.existsById("XYZ")).thenReturn(false);
        assertThatThrownBy(() -> servico.salvar(10L, pedido(null, de("XYZ", "1")), ADMIN))
                .hasMessageContaining("Tipo de hospedagem inválido: XYZ");

        when(tipos.existsById("HOTEL")).thenReturn(true);
        assertThatThrownBy(() -> servico.salvar(10L, pedido(null, de("HOTEL", "1"), de("HOTEL", "2")), ADMIN))
                .hasMessage("A diária de HOTEL foi enviada mais de uma vez.");
        assertThatThrownBy(() -> servico.salvar(10L, pedido(null, de(null, "1"), de(" ", "2")), ADMIN))
                .hasMessage("A diária geral foi enviada mais de uma vez.");
        verify(diarias, never()).save(any());
    }
}
