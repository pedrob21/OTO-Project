package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import jakarta.persistence.PersistenceException;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.usuario.Usuario;

class CampanhaRepositoryTest extends BaseRepositorioTest {

    private static final YearMonth JAN = YearMonth.of(2026, 1);
    private static final YearMonth FEV = YearMonth.of(2026, 2);
    private static final YearMonth MAR = YearMonth.of(2026, 3);

    @Autowired
    private CampanhaRepository repository;

    private Usuario admin;
    private RodadaPesquisa abertaA;
    private RodadaPesquisa rascunhoB;
    private RodadaPesquisa encerradaC;

    private void rodadas() {
        admin = usuario("admin@oto.com");
        abertaA = rodada("001/26", StatusRodada.ABERTA, admin);
        rascunhoB = rodada("002/26", StatusRodada.RASCUNHO, admin);
        encerradaC = rodada("003/26", StatusRodada.ENCERRADA, admin);
    }

    private Campanha publicada(RodadaPesquisa rodada, YearMonth mes, Modalidade modalidade) {
        Campanha c = campanhaMensal(rodada, mes, modalidade);
        c.publicar(admin.getId());
        return gravar(c);
    }

    @Test
    void publicadasVemDaMaisRecenteComDesempatePeloIdMaisNovoEComARodadaCarregada() {
        rodadas();
        Campanha janConsolidada = publicada(encerradaC, JAN, Modalidade.CONSOLIDADA);
        Campanha marConsolidada = publicada(abertaA, MAR, Modalidade.CONSOLIDADA);
        Campanha marExpectativa = publicada(abertaA, MAR, Modalidade.EXPECTATIVA);
        campanhaMensal(abertaA, FEV, Modalidade.CONSOLIDADA);
        gravarELimpar();

        List<Campanha> publicadas = repository.findPublicadas();

        assertThat(publicadas).extracting(Campanha::getId).containsExactly(marExpectativa.getId(),
                marConsolidada.getId(), janConsolidada.getId());
        assertThat(publicadas).allSatisfy(c -> assertThat(Hibernate.isInitialized(c.getRodada())).isTrue());
    }

    @Test
    void campanhaSemPublicacaoNaoApareceEntreAsPublicadas() {
        rodadas();
        campanhaMensal(abertaA, JAN, Modalidade.CONSOLIDADA);
        gravarELimpar();

        assertThat(repository.findPublicadas()).isEmpty();
    }

    @Test
    void campanhasPorStatusDaRodadaFiltramOStatusEOrdenamPeloInicioDoPeriodo() {
        rodadas();
        Campanha marA = campanhaMensal(abertaA, MAR, Modalidade.CONSOLIDADA);
        Campanha janA = campanhaMensal(abertaA, JAN, Modalidade.CONSOLIDADA);
        campanhaMensal(rascunhoB, FEV, Modalidade.CONSOLIDADA);
        campanhaMensal(encerradaC, FEV, Modalidade.EXPECTATIVA);
        gravarELimpar();

        List<Campanha> abertas = repository.findByStatusDaRodada(StatusRodada.ABERTA);

        assertThat(abertas).extracting(Campanha::getId).containsExactly(janA.getId(), marA.getId());
        assertThat(abertas).allSatisfy(c -> assertThat(Hibernate.isInitialized(c.getRodada())).isTrue());
        assertThat(repository.findByStatusDaRodada(StatusRodada.RASCUNHO)).hasSize(1);
        assertThat(repository.findByStatusDaRodada(StatusRodada.ENCERRADA)).hasSize(1);
    }

    @Test
    void campanhasDeUmaRodadaVemEmOrdemDeInicio() {
        rodadas();
        Campanha mar = campanhaMensal(abertaA, MAR, Modalidade.CONSOLIDADA);
        Campanha jan = campanhaMensal(abertaA, JAN, Modalidade.CONSOLIDADA);
        campanhaMensal(rascunhoB, FEV, Modalidade.CONSOLIDADA);
        gravarELimpar();

        assertThat(repository.findByRodadaIdOrderByPeriodoInicio(abertaA.getId())).extracting(Campanha::getId)
                .containsExactly(jan.getId(), mar.getId());
    }

    @Test
    void campanhasDeVariasRodadasSoTrazemAsRodadasPedidas() {
        rodadas();
        campanhaMensal(abertaA, JAN, Modalidade.CONSOLIDADA);
        campanhaMensal(rascunhoB, FEV, Modalidade.CONSOLIDADA);
        campanhaMensal(encerradaC, MAR, Modalidade.CONSOLIDADA);
        gravarELimpar();

        assertThat(repository.findByRodadaIdIn(List.of(abertaA.getId(), encerradaC.getId())))
                .extracting(Campanha::getNome)
                .containsExactlyInAnyOrder("Campanha 2026-01 CONSOLIDADA", "Campanha 2026-03 CONSOLIDADA");
    }

    @Test
    void buscaPorModalidadeTipoEInicioDiferenciaAModalidade() {
        rodadas();
        Campanha consolidada = campanhaMensal(abertaA, JAN, Modalidade.CONSOLIDADA);
        Campanha expectativa = campanhaMensal(abertaA, JAN, Modalidade.EXPECTATIVA);
        gravarELimpar();

        assertThat(repository.findByModalidadeAndTipoPeriodoAndPeriodoInicio(Modalidade.EXPECTATIVA,
                TipoPeriodo.MENSAL, JAN.atDay(1))).singleElement().extracting(Campanha::getId)
                .isEqualTo(expectativa.getId());
        assertThat(repository.findByModalidadeAndTipoPeriodoAndPeriodoInicio(Modalidade.CONSOLIDADA,
                TipoPeriodo.MENSAL, JAN.atDay(1))).singleElement().extracting(Campanha::getId)
                .isEqualTo(consolidada.getId());
        assertThat(repository.findByModalidadeAndTipoPeriodoAndPeriodoInicio(Modalidade.CONSOLIDADA,
                TipoPeriodo.FERIADO, JAN.atDay(1))).isEmpty();
        assertThat(repository.findByModalidadeAndTipoPeriodoAndPeriodoInicio(Modalidade.CONSOLIDADA,
                TipoPeriodo.MENSAL, FEV.atDay(1))).isEmpty();
    }

    @Test
    void rodadaTemPublicacaoSoQuandoAlgumaCampanhaDelaFoiPublicada() {
        rodadas();
        campanhaMensal(abertaA, JAN, Modalidade.CONSOLIDADA);
        publicada(encerradaC, FEV, Modalidade.CONSOLIDADA);
        campanhaMensal(encerradaC, MAR, Modalidade.CONSOLIDADA);
        gravarELimpar();

        assertThat(repository.existsByRodadaIdAndPublicadaEmIsNotNull(abertaA.getId())).isFalse();
        assertThat(repository.existsByRodadaIdAndPublicadaEmIsNotNull(encerradaC.getId())).isTrue();
        assertThat(repository.existsByRodadaIdAndPublicadaEmIsNotNull(rascunhoB.getId())).isFalse();
    }

    @Test
    void mesmoPeriodoNaMesmaModalidadeViolaAUnicidadeMesmoSemFeriado() {
        rodadas();
        campanhaMensal(abertaA, JAN, Modalidade.CONSOLIDADA);

        assertThatThrownBy(() -> campanhaMensal(rascunhoB, JAN, Modalidade.CONSOLIDADA))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("uk_campanha_periodo");
    }

    @Test
    void feriadoPrecisaDeCodigoEMensalNaoPodeTer() {
        rodadas();
        LocalDate inicio = LocalDate.of(2026, 2, 14);

        assertThatThrownBy(() -> gravar(new Campanha(abertaA, new DadosCampanha("Sem código", Modalidade.CONSOLIDADA,
                TipoPeriodo.FERIADO, null, inicio, inicio.plusDays(4), 4))))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_campanha_feriado");
    }

    @Test
    void feriadoComCodigoDaTabelaDeReferenciaEhAceitoEDiferenciaDoMensalNaUnicidade() {
        rodadas();
        LocalDate inicio = LocalDate.of(2026, 2, 14);
        campanhaMensal(abertaA, FEV, Modalidade.CONSOLIDADA);

        Campanha carnaval = gravar(new Campanha(abertaA, new DadosCampanha("Carnaval", Modalidade.CONSOLIDADA,
                TipoPeriodo.FERIADO, "CARNAVAL", inicio, inicio.plusDays(4), 4)));
        gravarELimpar();

        assertThat(repository.findById(carnaval.getId()).orElseThrow().getFeriadoCodigo()).isEqualTo("CARNAVAL");
    }

    @Test
    void periodoNaoPodeTerminarAntesDeComecar() {
        rodadas();

        assertThatThrownBy(() -> gravar(new Campanha(abertaA, new DadosCampanha("Invertida", Modalidade.CONSOLIDADA,
                TipoPeriodo.MENSAL, null, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 1), 31))))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_campanha_periodo");
    }

    @Test
    void campanhaDeUmDiaSoEhValida() {
        rodadas();
        LocalDate dia = LocalDate.of(2026, 1, 1);

        Campanha c = gravar(new Campanha(abertaA, new DadosCampanha("Um dia", Modalidade.CONSOLIDADA,
                TipoPeriodo.MENSAL, null, dia, dia, 1)));
        gravarELimpar();

        assertThat(repository.findById(c.getId())).isPresent();
    }

    @Test
    void quantidadeDeDiariasPrecisaSerPositiva() {
        rodadas();

        assertThatThrownBy(() -> gravar(new Campanha(abertaA, new DadosCampanha("Sem diárias", Modalidade.CONSOLIDADA,
                TipoPeriodo.MENSAL, null, JAN.atDay(1), JAN.atEndOfMonth(), 0))))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_campanha_diarias");
    }
}
