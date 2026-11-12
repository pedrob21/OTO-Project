package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import OTO.Fixtures;
import OTO.calculo.BaseCalculo.Participante;
import OTO.calculo.CalculadoraOcupacao.Entrada;
import OTO.calculo.CalculadoraOcupacao.MeioHospedagem;
import OTO.estabelecimento.Estabelecimento;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaDiaria;
import OTO.pesquisa.StatusRodada;
import OTO.referencia.TipoHospedagem;

class BaseCalculoTest {

    private static final YearMonth FEV = YearMonth.of(2026, 2);
    private static final LocalDate INICIO = FEV.atDay(1);

    private final Campanha campanha = Fixtures.campanhaMensal(10, Fixtures.rodada(1, "X", StatusRodada.ENCERRADA), FEV);
    private final Estabelecimento a = Fixtures.estabelecimento(1, "A", "HOTEL");
    private final Estabelecimento b = Fixtures.estabelecimento(2, "B", "POUSADA");
    private final List<Participante> participantes = List.of(
            new Participante(a, Fixtures.capacidade(1, a, INICIO, 10, 30, 4), Fixtures.resposta(1, campanha, a, "80", "500")),
            new Participante(b, Fixtures.capacidade(2, b, INICIO, 10, 25, 4), null));

    private BaseCalculo base(boolean comRhc, List<CampanhaDiaria> diarias) {
        return new BaseCalculo(campanha, FEV.atEndOfMonth(), participantes, List.of(), List.of(),
                comRhc ? Fixtures.rhc(1, INICIO, 10, 4, 12, 3) : null, Fixtures.parametro(1, INICIO),
                List.of(new TipoHospedagem("HOTEL", "Hotel", true, 1), new TipoHospedagem("FLAT", "Flat", false, 2)), diarias);
    }

    @Test
    void contaRespondentesELeitos() {
        BaseCalculo base = base(true, List.of());
        assertThat(base.qtdRespondentes()).isEqualTo(1);
        assertThat(base.leitosTotal()).isEqualTo(30 + 25 + 12);
        assertThat(base.leitosRespondentes()).isEqualTo(30);
        assertThat(base.percentualLeitosRespondentes()).isEqualTo(100.0 * 30 / 67);
        assertThat(base(false, List.of()).leitosTotal()).isEqualTo(55);
    }

    @Test
    void semLeitosORepresentatividadeEhZero() {
        BaseCalculo vazia = new BaseCalculo(campanha, FEV.atEndOfMonth(), List.of(), List.of(), List.of(), null,
                Fixtures.parametro(1, INICIO), List.of(), List.of());
        assertThat(vazia.percentualLeitosRespondentes()).isZero();
    }

    @Test
    void paraEntradaConverteParticipantesParametrosERhc() {
        Entrada entrada = base(true, List.of()).paraEntrada();

        assertThat(entrada.meios()).containsExactly(
                new MeioHospedagem(1, "HOTEL", 10, 30, 4, 80.0, 500.0),
                new MeioHospedagem(2, "POUSADA", 10, 25, 4, null, null));
        assertThat(entrada.rhc()).isEqualTo(new CalculadoraOcupacao.HospedagemCaseira(4, 12, 3));
        assertThat(entrada.qtdDiarias()).isEqualTo(28);
        assertThat(entrada.parametros()).isEqualTo(new CalculadoraOcupacao.Parametros(15.15, 4, 2, 250.0, 105.0));
        assertThat(entrada.tipos()).containsExactly(new CalculadoraOcupacao.Tipo("HOTEL", true),
                new CalculadoraOcupacao.Tipo("FLAT", false));
        assertThat(entrada.diariasInformadas()).isEmpty();
        assertThat(entrada.diariaGeralInformada()).isNull();
    }

    @Test
    void semRhcUsaHospedagemCaseiraVaziaEDiariasDigitadasEntram() {
        Entrada entrada = base(false, List.of(
                new CampanhaDiaria(10L, null, new BigDecimal("420.00"), null, 1L),
                new CampanhaDiaria(10L, "HOTEL", new BigDecimal("450.00"), null, 1L))).paraEntrada();

        assertThat(entrada.rhc()).isEqualTo(CalculadoraOcupacao.HospedagemCaseira.NENHUMA);
        assertThat(entrada.diariaGeralInformada()).isEqualTo(420.0);
        assertThat(entrada.diariasInformadas()).containsExactlyEntriesOf(java.util.Map.of("HOTEL", 450.0));
    }
}
