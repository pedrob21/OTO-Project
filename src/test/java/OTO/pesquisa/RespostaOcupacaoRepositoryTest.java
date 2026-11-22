package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import jakarta.persistence.PersistenceException;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.estabelecimento.Estabelecimento;
import OTO.usuario.Usuario;

class RespostaOcupacaoRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private RespostaOcupacaoRepository repository;

    private Usuario autor;
    private Estabelecimento alfa;
    private Estabelecimento beta;
    private Estabelecimento gama;
    private Campanha jan;
    private Campanha fevConsolidada;
    private Campanha fevExpectativa;

    private void cenario() {
        autor = usuario("autor@oto.com");
        alfa = estabelecimento("Alfa", "HOTEL", "11111111000101");
        beta = estabelecimento("Beta", "POUSADA", "11111111000102");
        gama = estabelecimento("Gama", "RESORT", "11111111000103");
        RodadaPesquisa rodada = rodada("001/26", StatusRodada.ABERTA, autor);
        jan = campanhaMensal(rodada, YearMonth.of(2026, 1), Modalidade.CONSOLIDADA);
        fevConsolidada = campanhaMensal(rodada, YearMonth.of(2026, 2), Modalidade.CONSOLIDADA);
        fevExpectativa = campanhaMensal(rodada, YearMonth.of(2026, 2), Modalidade.EXPECTATIVA);
    }

    @Test
    void buscaARespostaDoEstabelecimentoNaCampanha() {
        cenario();
        resposta(jan, alfa, "55.50", autor);
        resposta(jan, beta, "70.00", autor);
        gravarELimpar();

        assertThat(repository.findByCampanhaIdAndEstabelecimentoId(jan.getId(), alfa.getId()).orElseThrow()
                .getTaxaOcupacao()).isEqualByComparingTo("55.50");
        assertThat(repository.findByCampanhaIdAndEstabelecimentoId(fevConsolidada.getId(), alfa.getId())).isEmpty();
        assertThat(repository.findByCampanhaIdAndEstabelecimentoId(jan.getId(), gama.getId())).isEmpty();
    }

    @Test
    void campanhaTemRespostaSoQuandoAlguemRespondeu() {
        cenario();
        resposta(jan, alfa, "55.50", autor);
        gravarELimpar();

        assertThat(repository.existsByCampanhaId(jan.getId())).isTrue();
        assertThat(repository.existsByCampanhaId(fevConsolidada.getId())).isFalse();
    }

    @Test
    void respostasDaCampanhaTrazemOsEstabelecimentosSemMisturarCampanhas() {
        cenario();
        resposta(jan, alfa, "55.50", autor);
        resposta(jan, beta, "70.00", autor);
        resposta(fevConsolidada, gama, "30.00", autor);
        gravarELimpar();

        List<RespostaOcupacao> respostas = repository.findByCampanhaIdComEstabelecimento(jan.getId());

        assertThat(respostas).extracting(r -> r.getEstabelecimento().getNomeFantasia())
                .containsExactlyInAnyOrder("Alfa", "Beta");
        assertThat(respostas).allSatisfy(r -> assertThat(Hibernate.isInitialized(r.getEstabelecimento())).isTrue());
    }

    @Test
    void respostasDeVariasCampanhasSoTrazemAsPedidas() {
        cenario();
        resposta(jan, alfa, "55.50", autor);
        resposta(fevConsolidada, beta, "40.00", autor);
        resposta(fevExpectativa, gama, "30.00", autor);
        gravarELimpar();

        List<RespostaOcupacao> respostas = repository
                .findByCampanhaIdsComEstabelecimento(List.of(jan.getId(), fevExpectativa.getId()));

        assertThat(respostas).extracting(r -> r.getEstabelecimento().getNomeFantasia())
                .containsExactlyInAnyOrder("Alfa", "Gama");
        assertThat(respostas).allSatisfy(r -> assertThat(Hibernate.isInitialized(r.getEstabelecimento())).isTrue());
    }

    @Test
    void respostasDoParceiroSoSaoDosSeusEstabelecimentosDaCampanhaMaisRecenteParaAMaisAntiga() {
        cenario();
        RespostaOcupacao alfaJan = resposta(jan, alfa, "55.50", autor);
        RespostaOcupacao alfaFevCons = resposta(fevConsolidada, alfa, "60.00", autor);
        RespostaOcupacao betaFevExp = resposta(fevExpectativa, beta, "45.00", autor);
        resposta(jan, gama, "99.00", autor);
        gravarELimpar();

        List<RespostaOcupacao> respostas = repository
                .findByEstabelecimentoIdsComCampanha(List.of(alfa.getId(), beta.getId()));

        assertThat(respostas).extracting(RespostaOcupacao::getId)
                .containsExactly(betaFevExp.getId(), alfaFevCons.getId(), alfaJan.getId());
        assertThat(respostas).allSatisfy(r -> {
            assertThat(Hibernate.isInitialized(r.getCampanha())).isTrue();
            assertThat(Hibernate.isInitialized(r.getEstabelecimento())).isTrue();
        });
    }

    @Test
    void parceiroSemRespostasRecebeListaVazia() {
        cenario();
        resposta(jan, alfa, "55.50", autor);
        gravarELimpar();

        assertThat(repository.findByEstabelecimentoIdsComCampanha(List.of(gama.getId()))).isEmpty();
    }

    @Test
    void estabelecimentoSoRespondeUmaVezPorCampanha() {
        cenario();
        resposta(jan, alfa, "55.50", autor);

        assertThatThrownBy(() -> resposta(jan, alfa, "60.00", autor)).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_resposta_campanha_estab");
    }

    @Test
    void taxasDeZeroECemSaoAceitasEForaDoIntervaloNao() {
        cenario();
        resposta(jan, alfa, "0.00", autor);
        resposta(jan, beta, "100.00", autor);
        gravarELimpar();

        assertThat(repository.findByCampanhaIdComEstabelecimento(jan.getId())).extracting(RespostaOcupacao::getTaxaOcupacao)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("0.00"), new BigDecimal("100.00"));
        assertThatThrownBy(() -> resposta(jan, gama, "100.01", autor)).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_resposta_taxa");
    }

    @Test
    void taxaNegativaEhRejeitada() {
        cenario();

        assertThatThrownBy(() -> resposta(jan, alfa, "-0.01", autor)).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_resposta_taxa");
    }

    @Test
    void diariaMediaZeradaEhRejeitadaMasAusenteEhPermitida() {
        cenario();
        gravar(new RespostaOcupacao(jan, alfa, new BigDecimal("50.00"), null, autor.getId()));

        assertThatThrownBy(() -> gravar(new RespostaOcupacao(jan, beta, new BigDecimal("50.00"), BigDecimal.ZERO,
                autor.getId()))).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_resposta_diaria");
    }
}
