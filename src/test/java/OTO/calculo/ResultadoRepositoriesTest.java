package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.calculo.CalculadoraOcupacao.Resultado;
import OTO.calculo.CalculadoraOcupacao.ResultadoTipo;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.ParametroCalculo;
import OTO.pesquisa.RodadaPesquisa;
import OTO.pesquisa.StatusRodada;
import OTO.usuario.Usuario;

class ResultadoRepositoriesTest extends BaseRepositorioTest {

    @Autowired
    private ResultadoCampanhaRepository resultados;

    @Autowired
    private ResultadoCampanhaTipoRepository tipos;

    @Autowired
    private ResultadoCampanhaFaixaRepository faixas;

    private Usuario admin;
    private ParametroCalculo parametroUsado;
    private ParametroCalculo parametroLivre;
    private Campanha jan;
    private Campanha fev;

    private void cenario() {
        admin = usuario("admin@oto.com");
        parametroUsado = gravar(new ParametroCalculo(LocalDate.of(2026, 1, 1), new BigDecimal("15.00"), 4, 2,
                new BigDecimal("250.00"), new BigDecimal("105.00"), new BigDecimal("30.00"), "Fonte", admin.getId()));
        parametroLivre = gravar(new ParametroCalculo(LocalDate.of(2026, 6, 1), new BigDecimal("15.00"), 4, 2,
                new BigDecimal("250.00"), new BigDecimal("105.00"), new BigDecimal("30.00"), "Fonte", admin.getId()));
        RodadaPesquisa rodada = rodada("001/26", StatusRodada.ENCERRADA, admin);
        jan = campanhaMensal(rodada, YearMonth.of(2026, 1), Modalidade.CONSOLIDADA);
        fev = campanhaMensal(rodada, YearMonth.of(2026, 2), Modalidade.CONSOLIDADA);
    }

    private Resultado calculo(int qtdMh, int respondentes) {
        return new Resultado(55.5, qtdMh, respondentes, 1000, 700, 70.0, 80.0, 1000.0, 100.0, 50.0, 1150.0, 37.1,
                300.0, 2000.0, 3000.0, List.of());
    }

    private ResultadoCampanha resultado(Campanha c, ParametroCalculo p, int qtdMh, int respondentes) {
        return gravar(ResultadoCampanha.de(c.getId(), p.getId(), calculo(qtdMh, respondentes)));
    }

    @Test
    void resultadosPorCampanhaSoTrazemAsCampanhasPedidas() {
        cenario();
        resultado(jan, parametroUsado, 10, 8);
        resultado(fev, parametroUsado, 20, 15);
        gravarELimpar();

        assertThat(resultados.findByCampanhaIdIn(List.of(fev.getId()))).extracting(ResultadoCampanha::getCampanhaId,
                ResultadoCampanha::getQtdMh).containsExactly(tuple(fev.getId(), 20));
        assertThat(resultados.findByCampanhaIdIn(List.of(jan.getId(), fev.getId()))).hasSize(2);
    }

    @Test
    void parametroSoEstaEmUsoQuandoUmResultadoReferenciaEle() {
        cenario();
        resultado(jan, parametroUsado, 10, 8);
        gravarELimpar();

        assertThat(resultados.existsByParametroCalculoId(parametroUsado.getId())).isTrue();
        assertThat(resultados.existsByParametroCalculoId(parametroLivre.getId())).isFalse();
    }

    @Test
    void respondentesNaoPodemPassarDoTotalDeMeiosDeHospedagem() {
        cenario();

        assertThatThrownBy(() -> resultado(jan, parametroUsado, 10, 11))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_resultado_mh");
    }

    @Test
    void resultadoPorTipoEhBuscadoEApagadoPorCampanha() {
        cenario();
        resultado(jan, parametroUsado, 10, 8);
        resultado(fev, parametroUsado, 10, 8);
        gravar(ResultadoCampanhaTipo.de(jan.getId(), new ResultadoTipo("HOTEL", 6, 5, 100, 60.0, 300.0, 180.0, 5000.0)));
        gravar(ResultadoCampanhaTipo.de(jan.getId(), new ResultadoTipo("POUSADA", 4, 3, 40, 50.0, 200.0, 100.0, 900.0)));
        gravar(ResultadoCampanhaTipo.de(fev.getId(), new ResultadoTipo("HOTEL", 6, 5, 100, 61.0, 300.0, 183.0, 5100.0)));
        gravarELimpar();

        assertThat(tipos.findByIdCampanhaId(jan.getId())).extracting(ResultadoCampanhaTipo::getTipoHospedagemCodigo)
                .containsExactlyInAnyOrder("HOTEL", "POUSADA");

        tipos.deleteByIdCampanhaId(jan.getId());
        gravarELimpar();

        assertThat(tipos.findByIdCampanhaId(jan.getId())).isEmpty();
        assertThat(tipos.findByIdCampanhaId(fev.getId())).hasSize(1);
    }

    @Test
    void resultadoPorFaixaEhBuscadoEApagadoPorCampanha() {
        cenario();
        resultado(jan, parametroUsado, 10, 8);
        resultado(fev, parametroUsado, 10, 8);
        gravar(new ResultadoCampanhaFaixa(jan.getId(), "BOA", 3));
        gravar(new ResultadoCampanhaFaixa(jan.getId(), "OTIMA", 5));
        gravar(new ResultadoCampanhaFaixa(fev.getId(), "BOA", 8));
        gravarELimpar();

        assertThat(faixas.findByIdCampanhaId(jan.getId()))
                .extracting(ResultadoCampanhaFaixa::getFaixaCodigo, ResultadoCampanhaFaixa::getQtdMh)
                .containsExactlyInAnyOrder(tuple("BOA", 3),
                        tuple("OTIMA", 5));

        faixas.deleteByIdCampanhaId(jan.getId());
        gravarELimpar();

        assertThat(faixas.findByIdCampanhaId(jan.getId())).isEmpty();
        assertThat(faixas.findByIdCampanhaId(fev.getId())).hasSize(1);
    }

    @Test
    void faixaInexistenteViolaAChaveEstrangeira() {
        cenario();
        resultado(jan, parametroUsado, 10, 8);

        assertThatThrownBy(() -> gravar(new ResultadoCampanhaFaixa(jan.getId(), "LENDARIA", 1)))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("resultado_campanha_faixa_faixa_codigo_fkey");
    }
}
