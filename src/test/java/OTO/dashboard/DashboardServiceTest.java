package OTO.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.calculo.CalculadoraOcupacao;
import OTO.calculo.ResultadoCampanha;
import OTO.calculo.ResultadoCampanhaRepository;
import OTO.calculo.ResultadoDtos.FaixaRef;
import OTO.comum.excecao.RegraNegocioException;
import OTO.dashboard.DashboardDtos.AcumuladoAno;
import OTO.dashboard.DashboardDtos.Comparativo;
import OTO.dashboard.DashboardDtos.FeriadoSerie;
import OTO.dashboard.DashboardDtos.Indicadores;
import OTO.dashboard.DashboardDtos.PontoAnual;
import OTO.dashboard.DashboardDtos.PontoMensal;
import OTO.dashboard.DashboardDtos.ResumoResponse;
import OTO.dashboard.DashboardDtos.SerieMensalResponse;
import OTO.historico.Indicador;
import OTO.historico.IndicadorHistorico;
import OTO.historico.IndicadorHistoricoRepository;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaRepository;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.RodadaPesquisa;
import OTO.pesquisa.StatusRodada;
import OTO.referencia.FaixaOcupacao;
import OTO.referencia.FaixaOcupacaoRepository;
import OTO.referencia.FeriadoEvento;
import OTO.referencia.FeriadoEventoRepository;

/**
 * Publicados no sistema: fevereiro/2026 (60%, 1.000 turistas, R$ 400) e carnaval/2026 (80%, 500, sem diária),
 * mais uma expectativa de janeiro/2026 que não entra na modalidade consolidada. A série histórica tem
 * fevereiro e carnaval de 2025, janeiro de 2026 e um fevereiro/2026 que o sistema substitui.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

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

    private final RodadaPesquisa rodada = Fixtures.rodada(1, "X", StatusRodada.ENCERRADA);
    private final Campanha fev = Fixtures.campanhaMensal(10, rodada, YearMonth.of(2026, 2));
    private final Campanha carnaval = Fixtures.campanhaFeriado(11, rodada, "CARNAVAL", LocalDate.of(2026, 2, 13),
            LocalDate.of(2026, 2, 18), Modalidade.CONSOLIDADA);
    private final Campanha expectativa = Fixtures.campanhaMensal(12, rodada, YearMonth.of(2026, 1), Modalidade.EXPECTATIVA);

    private static ResultadoCampanha resultado(long campanhaId, double taxa, double fluxo, Double diaria) {
        return ResultadoCampanha.de(campanhaId, 1L, new CalculadoraOcupacao.Resultado(taxa, 1, 1, 10, 10, 100, 100,
                fluxo, 0, 0, fluxo, fluxo / 10, diaria, null, 0, List.of()));
    }

    private static IndicadorHistorico hist(Indicador indicador, Modalidade modalidade, int ano, Integer mes,
            String feriado, String tipo, String valor) {
        IndicadorHistorico h = new IndicadorHistorico(indicador, modalidade, ano, mes, feriado, tipo);
        h.definirValor(new BigDecimal(valor), null, "Planilha");
        return h;
    }

    @BeforeEach
    void dados() {
        lenient().when(campanhas.findPublicadas()).thenReturn(List.of(carnaval, fev, expectativa));
        lenient().when(resultados.findByCampanhaIdIn(anyList())).thenReturn(List.of(
                resultado(10, 60, 1000, 400.0), resultado(11, 80, 500, null), resultado(12, 70, 900, 300.0)));
        lenient().when(historico.findAll()).thenReturn(List.of(
                hist(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2025, 2, null, null, "50"),
                hist(Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA, 2025, 2, null, null, "800"),
                hist(Indicador.DIARIA_MEDIA, Modalidade.CONSOLIDADA, 2025, 2, null, null, "320"),
                hist(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2026, 1, null, null, "55"),
                hist(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2026, 2, null, null, "10"),
                hist(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2025, null, "CARNAVAL", null, "70"),
                hist(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2025, 3, null, "HOTEL", "99"),
                hist(Indicador.TAXA_OCUPACAO, Modalidade.EXPECTATIVA, 2025, 3, null, null, "98")));
        lenient().when(faixas.findAllByOrderByPercentualMinimo()).thenReturn(List.of(
                new FaixaOcupacao("RUIM", "Ruim", BigDecimal.ZERO), new FaixaOcupacao("BOA", "Boa", new BigDecimal("41")),
                new FaixaOcupacao("OTIMA", "Ótima", new BigDecimal("61"))));
        lenient().when(feriados.findAllByOrderByOrdem()).thenReturn(List.of(
                new FeriadoEvento("CARNAVAL", "Carnaval", 1), new FeriadoEvento("PASCOA", "Páscoa", 2)));
    }

    // ------------------------------------------------------------------ resumo

    @Test
    void resumoComparaComOMesmoPeriodoDoAnoAnterior() {
        ResumoResponse resumo = servico.resumo(Modalidade.CONSOLIDADA);

        Indicadores mes = resumo.ultimoMes();
        assertThat(mes.campanhaId()).isEqualTo(10L);
        assertThat(mes.taxaOcupacao()).isEqualByComparingTo("60");
        assertThat(mes.faixa()).isEqualTo(new FaixaRef("BOA", "Boa"));
        assertThat(mes.anoAnterior()).isEqualTo(new Comparativo(2025, new BigDecimal("50"), new BigDecimal("800"),
                new BigDecimal("320"), new BigDecimal("10.00"), new BigDecimal("25.00"), new BigDecimal("25.00"),
                "HISTORICO"));

        Indicadores feriado = resumo.ultimoFeriado();
        assertThat(feriado.feriadoCodigo()).isEqualTo("CARNAVAL");
        assertThat(feriado.faixa()).isEqualTo(new FaixaRef("OTIMA", "Ótima"));
        Comparativo anterior = feriado.anoAnterior();
        assertThat(anterior.diferencaTaxaPontos()).isEqualByComparingTo("10");
        assertThat(anterior.fluxoTotal()).isNull();
        assertThat(anterior.variacaoFluxoPercentual()).isNull();
        assertThat(anterior.variacaoDiariaPercentual()).isNull();
    }

    @Test
    void acumuladoDoAnoJuntaSistemaESerieHistorica() {
        AcumuladoAno acumulado = servico.resumo(Modalidade.CONSOLIDADA).acumuladoAno();
        // Janeiro 55 (histórico) e fevereiro 60 (sistema substitui os 10 do histórico)
        assertThat(acumulado).isEqualTo(new AcumuladoAno(2026, 2, new BigDecimal("57.50"), 1, new BigDecimal("1000.00")));
    }

    @Test
    void semPublicacaoNaoHaCartoes() {
        when(campanhas.findPublicadas()).thenReturn(List.of());
        ResumoResponse resumo = servico.resumo(Modalidade.CONSOLIDADA);
        assertThat(resumo.ultimoMes()).isNull();
        assertThat(resumo.ultimoFeriado()).isNull();
        assertThat(resumo.acumuladoAno()).isNull();
        verify(resultados, never()).findByCampanhaIdIn(anyList());
    }

    @Test
    void semDadoDoAnoAnteriorNaoHaComparativo() {
        when(historico.findAll()).thenReturn(List.of());
        assertThat(servico.resumo(Modalidade.CONSOLIDADA).ultimoMes().anoAnterior()).isNull();
    }

    @Test
    void anoAnteriorDoSistemaComFluxoZeroNaoTemVariacao() {
        Campanha fev2025 = Fixtures.campanhaMensal(13, rodada, YearMonth.of(2025, 2));
        when(campanhas.findPublicadas()).thenReturn(List.of(fev, fev2025));
        when(resultados.findByCampanhaIdIn(anyList())).thenReturn(List.of(resultado(10, 60, 1000, 400.0),
                resultado(13, 50, 0, null)));

        Comparativo anterior = servico.resumo(Modalidade.CONSOLIDADA).ultimoMes().anoAnterior();

        assertThat(anterior.origem()).isEqualTo("SISTEMA");
        assertThat(anterior.variacaoFluxoPercentual()).isNull();
        // Diária de 2025 veio do histórico; taxa e fluxo, do sistema
        assertThat(anterior.diariaMedia()).isEqualByComparingTo("320");
    }

    // ------------------------------------------------------------------ séries

    @Test
    void serieMensalComUmItemPorAno() {
        SerieMensalResponse serie = servico.serieMensal(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2025, 2026);

        assertThat(serie.anos()).hasSize(2);
        assertThat(serie.anos().get(0).meses()).hasSize(12);
        assertThat(serie.anos().get(0).meses().get(1)).isEqualTo(new PontoMensal(2, new BigDecimal("50"), "HISTORICO"));
        assertThat(serie.anos().get(0).meses().get(2)).isEqualTo(new PontoMensal(3, null, null));
        assertThat(serie.anos().get(1).meses().get(0)).isEqualTo(new PontoMensal(1, new BigDecimal("55"), "HISTORICO"));
        assertThat(serie.anos().get(1).meses().get(1).origem()).isEqualTo("SISTEMA");
        assertThat(serie.anos().get(1).meses().get(1).valor()).isEqualByComparingTo("60");
    }

    @Test
    void serieDaExpectativaUsaSoAExpectativa() {
        SerieMensalResponse serie = servico.serieMensal(Indicador.TAXA_OCUPACAO, Modalidade.EXPECTATIVA, 2025, 2026);
        assertThat(serie.anos().get(0).meses().get(2).valor()).isEqualByComparingTo("98");
        assertThat(serie.anos().get(1).meses().get(0).valor()).isEqualByComparingTo("70");
        assertThat(serie.anos().get(1).meses().get(1).valor()).isNull();
    }

    @Test
    void feriadosSemFiltroListamSoOsQueTemDados() {
        List<FeriadoSerie> series = servico.feriados(Modalidade.CONSOLIDADA, null, 2025, 2026);

        assertThat(series).extracting(FeriadoSerie::feriadoCodigo).containsExactly("CARNAVAL");
        List<PontoAnual> anos = series.get(0).anos();
        assertThat(anos.get(0)).isEqualTo(new PontoAnual(2025, new BigDecimal("70"), null, null, "HISTORICO"));
        assertThat(anos.get(1).origem()).isEqualTo("SISTEMA");
        assertThat(anos.get(1).fluxoTotal()).isEqualByComparingTo("500");
    }

    @Test
    void feriadoEscolhidoApareceMesmoSemDados() {
        List<FeriadoSerie> series = servico.feriados(Modalidade.CONSOLIDADA, " PASCOA ", 2025, 2026);
        assertThat(series).extracting(FeriadoSerie::nome).containsExactly("Páscoa");
        assertThat(series.get(0).anos()).extracting(PontoAnual::origem).containsOnlyNulls();
    }

    @Test
    void intervaloDeAnosValido() {
        assertThatThrownBy(() -> servico.serieMensal(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2026, 2025))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("ano inicial");
        assertThatThrownBy(() -> servico.feriados(Modalidade.CONSOLIDADA, null, 2000, 2020))
                .hasMessage("Escolha no máximo 20 anos.");
        assertThatThrownBy(() -> servico.serieMensal(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 1999, 2001))
                .hasMessage("Anos válidos: de 2000 a 2100.");
        verify(campanhas, never()).findPublicadas();
    }
}
