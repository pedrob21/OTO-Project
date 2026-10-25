package OTO.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import OTO.calculo.CalculadoraOcupacao.Entrada;
import OTO.calculo.CalculadoraOcupacao.Faixa;
import OTO.calculo.CalculadoraOcupacao.HospedagemCaseira;
import OTO.calculo.CalculadoraOcupacao.MeioHospedagem;
import OTO.calculo.CalculadoraOcupacao.Parametros;
import OTO.calculo.CalculadoraOcupacao.Resultado;
import OTO.calculo.CalculadoraOcupacao.ResultadoTipo;
import OTO.calculo.CalculadoraOcupacao.Tipo;

/**
 * Cenário pequeno, conferido à mão com as fórmulas da planilha de tabulação do OTO
 * (abas MH e TABULAÇÃO). Dois hotéis responderam; a pousada não.
 */
class CalculadoraOcupacaoTest {

    private static final double DELTA = 1e-6;

    private static final Parametros PARAMETROS = new Parametros(15.15, 4, 2, 250.0, 105.0);

    private static final List<Tipo> TIPOS = List.of(
            new Tipo("HOTEL", true), new Tipo("RESORT", true), new Tipo("POUSADA", true),
            new Tipo("HOTEL_FAZENDA", false), new Tipo("FLAT", false), new Tipo("HOSTEL", false));

    private static final List<Faixa> FAIXAS = List.of(
            new Faixa("RUIM", 0), new Faixa("REGULAR", 21), new Faixa("BOA", 41),
            new Faixa("OTIMA", 61), new Faixa("EXCELENTE", 81));

    // Leitos úteis: A = 10 x (1+4)/2 = 25; B = 20 x (1+3)/2 = 40; C = 10 x (1+4)/2 = 25; RHC = 4 x (1+3)/2 = 8
    private static final MeioHospedagem HOTEL_A = new MeioHospedagem(1, "HOTEL", 10, 30, 4, 80.0, 500.0);
    private static final MeioHospedagem HOTEL_B = new MeioHospedagem(2, "HOTEL", 20, 50, 3, 50.0, 400.0);
    private static final MeioHospedagem POUSADA_C = new MeioHospedagem(3, "POUSADA", 10, 25, 4, null, null);
    private static final HospedagemCaseira RHC = new HospedagemCaseira(4, 12, 3);

    private static Resultado calcular(List<MeioHospedagem> meios, int diarias) {
        return CalculadoraOcupacao.calcular(new Entrada(meios, RHC, diarias, PARAMETROS, TIPOS));
    }

    @Test
    void taxaGeralEhAMediaPonderadaPelasUh() {
        Resultado r = calcular(List.of(HOTEL_A, HOTEL_B, POUSADA_C), 2);
        // (80 x 10 + 50 x 20) / 30
        assertEquals(60.0, r.taxaOcupacao(), DELTA);
        assertEquals(3, r.qtdMh());
        assertEquals(2, r.qtdMhRespondentes());
    }

    @Test
    void fluxoSegueAMetodologiaDaPlanilha() {
        Resultado r = calcular(List.of(HOTEL_A, HOTEL_B, POUSADA_C), 2);
        // Respondentes: 25 x 0,8 x 2 + 40 x 0,5 x 2 = 80; não respondentes: (90 - 65) x 0,6 x 2 = 30
        assertEquals(110.0, r.fluxoMh(), DELTA);
        // RHC: 8 x 0,6 x 2
        assertEquals(9.6, r.fluxoRhc(), DELTA);
        // Day use: hospedados por dia 59,8 equivalem a 84,85% do total por dia
        double totalPorDia = 59.8 / 0.8485;
        assertEquals((totalPorDia - 59.8) * 2, r.fluxoDayUse(), DELTA);
        assertEquals(110.0 + 9.6 + (totalPorDia - 59.8) * 2, r.fluxoTotal(), DELTA);
        assertEquals(totalPorDia, r.fluxoMedioDiario(), DELTA);
    }

    @Test
    void representatividadeUsaLeitosDeclaradosDeMhERhc() {
        Resultado r = calcular(List.of(HOTEL_A, HOTEL_B, POUSADA_C), 2);
        assertEquals(30 + 50 + 25 + 12, r.leitosTotal());
        assertEquals(80, r.leitosRespondentes());
        assertEquals(100.0 * 80 / 117, r.percentualLeitosRespondentes(), DELTA);
        assertEquals(100.0 * 2 / 3, r.percentualMhRespondentes(), DELTA);
    }

    @Test
    void receitaPorTipoUsaDiariaPonderadaPelosLeitos() {
        Resultado r = calcular(List.of(HOTEL_A, HOTEL_B, POUSADA_C), 2);
        ResultadoTipo hotel = r.porTipo().get(0);
        assertEquals("HOTEL", hotel.tipo());
        assertEquals(60.0, hotel.taxaOcupacao(), DELTA);
        // (500 x 30 + 400 x 50) / 80
        assertEquals(437.5, hotel.diariaMedia(), DELTA);
        assertEquals(262.5, hotel.revpar(), DELTA);
        // RevPAR x 30 UH x 2 diárias
        assertEquals(15750.0, hotel.receitaEstimada(), DELTA);

        ResultadoTipo pousada = r.porTipo().get(1);
        assertEquals("POUSADA", pousada.tipo());
        assertEquals(0, pousada.qtdMhRespondentes());
        assertNull(pousada.taxaOcupacao());
        assertNull(pousada.receitaEstimada());

        assertEquals(15750.0, r.receitaHospedagem(), DELTA);
        assertEquals(437.5, r.diariaMedia(), DELTA);
        // Fluxo total x (105 x 2 dias em parques + 250 x 4 dias de permanência)
        assertEquals(r.fluxoTotal() * 1210, r.receitaSemHospedagem(), DELTA);
    }

    @Test
    void diariaDigitadaPelaEquipeSubstituiAMediaDasRespostas() {
        // Como na planilha, a equipe digita a diária (ex.: pesquisa no Booking) por tipo e a geral
        Resultado r = CalculadoraOcupacao.calcular(new Entrada(List.of(HOTEL_A, HOTEL_B, POUSADA_C), RHC, 2,
                PARAMETROS, TIPOS, Map.of("HOTEL", 450.0, "POUSADA", 300.0), 420.0));
        ResultadoTipo hotel = r.porTipo().get(0);
        assertEquals(450.0, hotel.diariaMedia(), DELTA);
        // RevPAR = 450 x 60%; receita = RevPAR x 30 UH x 2 diárias
        assertEquals(270.0, hotel.revpar(), DELTA);
        assertEquals(16200.0, hotel.receitaEstimada(), DELTA);
        // A pousada não respondeu: fica com a diária digitada, mas sem taxa não há receita
        ResultadoTipo pousada = r.porTipo().get(1);
        assertEquals(300.0, pousada.diariaMedia(), DELTA);
        assertNull(pousada.receitaEstimada());
        assertEquals(16200.0, r.receitaHospedagem(), DELTA);
        assertEquals(420.0, r.diariaMedia(), DELTA);
        // Taxa e fluxo continuam vindo só das respostas
        assertEquals(60.0, r.taxaOcupacao(), DELTA);
        assertEquals(110.0, r.fluxoMh(), DELTA);
    }

    @Test
    void tiposSemMeiosDeHospedagemNaoAparecem() {
        Resultado r = calcular(List.of(HOTEL_A, HOTEL_B, POUSADA_C), 2);
        assertEquals(2, r.porTipo().size());
    }

    @Test
    void tipoQueNaoCalculaReceitaNaoTemRevpar() {
        MeioHospedagem flat = new MeioHospedagem(4, "FLAT", 10, 30, 4, 70.0, 300.0);
        Resultado r = calcular(List.of(HOTEL_A, flat), 2);
        ResultadoTipo tipoFlat = r.porTipo().stream().filter(t -> t.tipo().equals("FLAT")).findFirst().orElseThrow();
        assertEquals(70.0, tipoFlat.taxaOcupacao(), DELTA);
        assertEquals(300.0, tipoFlat.diariaMedia(), DELTA);
        assertNull(tipoFlat.revpar());
        assertNull(tipoFlat.receitaEstimada());
    }

    @Test
    void semRhcOFluxoDaHospedagemCaseiraEhZero() {
        Resultado r = CalculadoraOcupacao.calcular(
                new Entrada(List.of(HOTEL_A, HOTEL_B), null, 2, PARAMETROS, TIPOS));
        assertEquals(0.0, r.fluxoRhc(), DELTA);
        assertEquals(80, r.leitosTotal());
    }

    @Test
    void distribuicaoPorFaixaSegueOsLimitesDosBoletins() {
        List<MeioHospedagem> meios = List.of(
                new MeioHospedagem(1, "HOTEL", 10, 30, 4, 0.0, null),
                new MeioHospedagem(2, "HOTEL", 10, 30, 4, 20.99, null),
                new MeioHospedagem(3, "HOTEL", 10, 30, 4, 21.0, null),
                new MeioHospedagem(4, "HOTEL", 10, 30, 4, 60.99, null),
                new MeioHospedagem(5, "HOTEL", 10, 30, 4, 81.0, null),
                new MeioHospedagem(6, "HOTEL", 10, 30, 4, 100.0, null),
                new MeioHospedagem(7, "HOTEL", 10, 30, 4, null, null));
        Map<String, Integer> faixas = CalculadoraOcupacao.distribuirPorFaixa(meios, FAIXAS);
        assertEquals(2, faixas.get("RUIM").intValue());
        assertEquals(1, faixas.get("REGULAR").intValue());
        assertEquals(1, faixas.get("BOA").intValue());
        assertEquals(0, faixas.get("OTIMA").intValue());
        assertEquals(2, faixas.get("EXCELENTE").intValue());
        assertEquals("OTIMA", CalculadoraOcupacao.faixaDe(80.0, FAIXAS).codigo());
    }

    @Test
    void semRespondentesNaoHaComoCalcular() {
        assertThrows(IllegalArgumentException.class, () -> calcular(List.of(POUSADA_C), 2));
    }

    @Test
    void diariasPrecisamSerPositivas() {
        assertThrows(IllegalArgumentException.class, () -> calcular(List.of(HOTEL_A), 0));
    }
}
