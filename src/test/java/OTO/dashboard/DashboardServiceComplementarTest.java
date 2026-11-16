package OTO.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.calculo.ResultadoCampanhaRepository;
import OTO.dashboard.DashboardDtos.FeriadoSerie;
import OTO.dashboard.DashboardDtos.PontoAnual;
import OTO.dashboard.DashboardDtos.PontoMensal;
import OTO.historico.Indicador;
import OTO.historico.IndicadorHistorico;
import OTO.historico.IndicadorHistoricoRepository;
import OTO.pesquisa.CampanhaRepository;
import OTO.pesquisa.Modalidade;
import OTO.referencia.FaixaOcupacaoRepository;
import OTO.referencia.FeriadoEvento;
import OTO.referencia.FeriadoEventoRepository;

/**
 * Só série histórica, sem nada publicado no sistema: março/2025 tem fluxo e diária (sem taxa),
 * o carnaval/2025 só fluxo e a páscoa/2025 só diária.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceComplementarTest {

    @Mock
    private CampanhaRepository campanhas;
    @Mock
    private ResultadoCampanhaRepository resultados;
    @Mock
    private IndicadorHistoricoRepository historico;
    @Mock
    private FaixaOcupacaoRepository faixas;
    @Mock
    private FeriadoEventoRepository feriados;

    @InjectMocks
    private DashboardService servico;

    private static IndicadorHistorico hist(Indicador indicador, Integer mes, String feriado, String valor) {
        IndicadorHistorico h = new IndicadorHistorico(indicador, Modalidade.CONSOLIDADA, 2025, mes, feriado, null);
        h.definirValor(new BigDecimal(valor), null, "Planilha");
        return h;
    }

    @BeforeEach
    void dados() {
        when(campanhas.findPublicadas()).thenReturn(List.of());
        when(historico.findAll()).thenReturn(List.of(
                hist(Indicador.FLUXO_TURISTAS, 3, null, "1000"),
                hist(Indicador.DIARIA_MEDIA, 3, null, "250"),
                hist(Indicador.FLUXO_TURISTAS, null, "CARNAVAL", "500"),
                hist(Indicador.DIARIA_MEDIA, null, "PASCOA", "300")));
    }

    @Test
    void serieMensalDeFluxoEDeDiaria() {
        PontoMensal fluxoMarco = servico.serieMensal(Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA, 2025, 2025)
                .anos().get(0).meses().get(2);
        PontoMensal diariaMarco = servico.serieMensal(Indicador.DIARIA_MEDIA, Modalidade.CONSOLIDADA, 2025, 2025)
                .anos().get(0).meses().get(2);

        assertThat(fluxoMarco.valor()).isEqualByComparingTo("1000");
        assertThat(fluxoMarco.origem()).isEqualTo("HISTORICO");
        assertThat(diariaMarco.valor()).isEqualByComparingTo("250");
    }

    @Test
    void feriadoSemTaxaAindaTemDado() {
        when(feriados.findAllByOrderByOrdem()).thenReturn(List.of(
                new FeriadoEvento("CARNAVAL", "Carnaval", 1), new FeriadoEvento("PASCOA", "Páscoa", 2)));

        List<FeriadoSerie> series = servico.feriados(Modalidade.CONSOLIDADA, null, 2025, 2025);

        assertThat(series).extracting(FeriadoSerie::feriadoCodigo).containsExactly("CARNAVAL", "PASCOA");
        PontoAnual carnaval = series.get(0).anos().get(0);
        assertThat(carnaval.taxaOcupacao()).isNull();
        assertThat(carnaval.fluxoTotal()).isEqualByComparingTo("500");
        PontoAnual pascoa = series.get(1).anos().get(0);
        assertThat(pascoa.fluxoTotal()).isNull();
        assertThat(pascoa.diariaMedia()).isEqualByComparingTo("300");
    }
}
