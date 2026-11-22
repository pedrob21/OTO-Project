package OTO.historico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.pesquisa.Modalidade;

class IndicadorHistoricoRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private IndicadorHistoricoRepository repository;

    private IndicadorHistorico valor(Indicador indicador, Modalidade modalidade, int ano, Integer mes, String feriado,
            String tipo, String valor) {
        IndicadorHistorico h = new IndicadorHistorico(indicador, modalidade, ano, mes, feriado, tipo);
        h.definirValor(new BigDecimal(valor), null, "Planilha histórica");
        return gravar(h);
    }

    private IndicadorHistorico taxa(int ano, Integer mes, String feriado, String tipo, String valor) {
        return valor(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, ano, mes, feriado, tipo, valor);
    }

    @Test
    void geraisFiltramIndicadorModalidadeEFaixaDeAnosInclusiva() {
        taxa(2022, 12, null, null, "10.00");
        taxa(2023, 3, null, null, "20.00");
        taxa(2024, 1, null, null, "30.00");
        taxa(2025, 1, null, null, "40.00");
        valor(Indicador.DIARIA_MEDIA, Modalidade.CONSOLIDADA, 2024, 1, null, null, "300.00");
        valor(Indicador.TAXA_OCUPACAO, Modalidade.EXPECTATIVA, 2024, 1, null, null, "99.00");
        gravarELimpar();

        assertThat(repository.findGerais(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, (short) 2023, (short) 2024))
                .extracting(IndicadorHistorico::getAno, IndicadorHistorico::getMes, h -> h.getValor().intValue())
                .containsExactly(tuple(2023, 3, 20), tuple(2024, 1, 30));
    }

    @Test
    void geraisExcluemValoresPorTipoDeHospedagem() {
        taxa(2024, 1, null, null, "30.00");
        taxa(2024, 1, null, "HOTEL", "55.00");
        gravarELimpar();

        assertThat(repository.findGerais(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, (short) 2024, (short) 2024))
                .singleElement().satisfies(h -> {
                    assertThat(h.getTipoHospedagemCodigo()).isNull();
                    assertThat(h.getValor()).isEqualByComparingTo("30.00");
                });
    }

    @Test
    void geraisOrdenamPorAnoEMesELevamOsFeriadosDepoisDosMesesDoAno() {
        taxa(2024, 11, null, null, "1.00");
        taxa(2024, null, "CARNAVAL", null, "2.00");
        taxa(2023, 12, null, null, "3.00");
        taxa(2024, 2, null, null, "4.00");
        gravarELimpar();

        assertThat(repository.findGerais(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, (short) 2023, (short) 2024))
                .extracting(IndicadorHistorico::getAno, IndicadorHistorico::getMes, IndicadorHistorico::getFeriadoCodigo)
                .containsExactly(tuple(2023, 12, null), tuple(2024, 2, null), tuple(2024, 11, null),
                        tuple(2024, null, "CARNAVAL"));
    }

    @Test
    void serieDoIndicadorVemPorAnoEMesAscendentesComTodosOsTipos() {
        taxa(2024, 2, null, "HOTEL", "1.00");
        taxa(2023, 12, null, null, "2.00");
        taxa(2024, 1, null, null, "3.00");
        valor(Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA, 2024, 1, null, null, "500.00");
        gravarELimpar();

        assertThat(repository.findByIndicadorAndModalidadeOrderByAnoAscMesAsc(Indicador.TAXA_OCUPACAO,
                Modalidade.CONSOLIDADA)).extracting(IndicadorHistorico::getAno, IndicadorHistorico::getMes)
                .containsExactly(tuple(2023, 12), tuple(2024, 1), tuple(2024, 2));
    }

    @Test
    void buscaPorAnoTrazSoOAnoPedidoDoIndicadorEModalidade() {
        taxa(2023, 5, null, null, "1.00");
        taxa(2024, 5, null, null, "2.00");
        taxa(2024, null, "NATAL", "HOTEL", "3.00");
        valor(Indicador.TAXA_OCUPACAO, Modalidade.EXPECTATIVA, 2024, 5, null, null, "4.00");
        gravarELimpar();

        assertThat(repository.findByIndicadorAndModalidadeAndAno(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA,
                (short) 2024)).extracting(h -> h.getValor().intValue()).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void combinacaoRepetidaComTipoNuloViolaAUnicidade() {
        taxa(2024, 1, null, null, "30.00");

        assertThatThrownBy(() -> taxa(2024, 1, null, null, "31.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_indicador_historico");
    }

    @Test
    void mesmaCombinacaoComTipoDiferenteOuModalidadeDiferenteEhPermitida() {
        taxa(2024, 1, null, null, "30.00");
        taxa(2024, 1, null, "HOTEL", "31.00");
        valor(Indicador.TAXA_OCUPACAO, Modalidade.EXPECTATIVA, 2024, 1, null, null, "32.00");
        gravarELimpar();

        assertThat(repository.findByIndicadorAndModalidadeAndAno(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA,
                (short) 2024)).hasSize(2);
    }

    @Test
    void periodoPrecisaSerMesOuFeriadoNuncaOsDoisNemNenhum() {
        assertThatThrownBy(() -> taxa(2024, null, null, null, "1.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_indicador_historico_periodo");
    }

    @Test
    void mesEFeriadoJuntosSaoRejeitados() {
        assertThatThrownBy(() -> taxa(2024, 1, "CARNAVAL", null, "1.00"))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_indicador_historico_periodo");
    }

    @Test
    void mesForaDeUmADozeEhRejeitado() {
        assertThatThrownBy(() -> taxa(2024, 13, null, null, "1.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_indicador_historico_mes");
    }

    @Test
    void anoAntesDe2000EhRejeitado() {
        assertThatThrownBy(() -> taxa(1999, 1, null, null, "1.00")).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_indicador_historico_ano");
    }

    @Test
    void feriadoForaDaTabelaDeReferenciaEhRejeitado() {
        assertThatThrownBy(() -> taxa(2024, null, "FERIADO_INVENTADO", null, "1.00"))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("indicador_historico_feriado_codigo_fkey");
    }
}
