package OTO.estabelecimento;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.estabelecimento.EstabelecimentoDtos.RhcRequest;
import OTO.estabelecimento.EstabelecimentoDtos.RhcResponse;

@ExtendWith(MockitoExtension.class)
class RhcServiceComplementarTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);

    @Mock
    private RhcResumoRepository rhc;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private RhcService servico;

    @Test
    void corrigirParaDataLivre() {
        RhcResumo atual = Fixtures.rhc(1, JAN, 40, 50, 120, 3);
        when(rhc.findById(1L)).thenReturn(Optional.of(atual));
        when(rhc.existsByVigenteDesde(JAN.plusDays(1))).thenReturn(false);

        RhcResponse depois = servico.corrigir(1L, new RhcRequest(JAN.plusDays(1), 40, 50, 120, 3), ADMIN);

        assertThat(depois.vigenteDesde()).isEqualTo(JAN.plusDays(1));
        assertThat(atual.getVigenteDesde()).isEqualTo(JAN.plusDays(1));
        verify(rhc).existsByVigenteDesde(JAN.plusDays(1));
    }
}
