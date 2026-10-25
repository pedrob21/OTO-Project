package OTO.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import OTO.calculo.ResultadoDtos.FaixaRef;
import OTO.historico.Indicador;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.TipoPeriodo;

/**
 * dados dos dashboards. Tudo é agregado da cidade. Os valores vêm dos resultados
 * publicados no sistema e, para períodos anteriores, da série histórica.
 * Em cada ponto, {@code origem} diz de onde veio: SISTEMA ou HISTORICO.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Cartões da tela inicial. Campos nulos = ainda não há resultado publicado. */
    public record ResumoResponse(Modalidade modalidade, Indicadores ultimoMes, Indicadores ultimoFeriado,
            AcumuladoAno acumuladoAno) {
    }

    public record Indicadores(
            Long campanhaId,
            String nome,
            TipoPeriodo tipoPeriodo,
            String feriadoCodigo,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            BigDecimal taxaOcupacao,
            FaixaRef faixa,
            BigDecimal fluxoTotal,
            BigDecimal fluxoMedioDiario,
            BigDecimal diariaMedia,
            BigDecimal receitaHospedagem,
            BigDecimal receitaSemHospedagem,
            BigDecimal percentualLeitosRespondentes,
            Comparativo anoAnterior) {
    }

    /**
     * Mesmo período do ano anterior.
     *
     * @param diferencaTaxaPontos diferença da taxa em pontos percentuais (ex.: 62% → 70% = +8)
     * @param variacaoFluxoPercentual variação do fluxo em % (ex.: +12,5)
     */
    public record Comparativo(
            int ano,
            BigDecimal taxaOcupacao,
            BigDecimal fluxoTotal,
            BigDecimal diariaMedia,
            BigDecimal diferencaTaxaPontos,
            BigDecimal variacaoFluxoPercentual,
            BigDecimal variacaoDiariaPercentual,
            String origem) {
    }

    /** Ano do último mês publicado: média das taxas mensais e soma dos fluxos mensais disponíveis. */
    public record AcumuladoAno(int ano, int mesesComTaxa, BigDecimal taxaOcupacaoMedia, int mesesComFluxo,
            BigDecimal fluxoTotal) {
    }

    /** Série mês a mês, um item por ano (para o gráfico de linhas comparando anos). */
    public record SerieMensalResponse(Indicador indicador, Modalidade modalidade, List<SerieAno> anos) {
    }

    public record SerieAno(int ano, List<PontoMensal> meses) {
    }

    /** {@code valor} nulo = sem dado para o mês. */
    public record PontoMensal(int mes, BigDecimal valor, String origem) {
    }

    /** Um feriado ao longo dos anos (para o gráfico de barras dos feriados). */
    public record FeriadoSerie(String feriadoCodigo, String nome, List<PontoAnual> anos) {
    }

    public record PontoAnual(int ano, BigDecimal taxaOcupacao, BigDecimal fluxoTotal, BigDecimal diariaMedia,
            String origem) {
    }
}
