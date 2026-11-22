package OTO.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Dados fictícios da demonstração: os meios de hospedagem, a sazonalidade de Olímpia e o calendário
 * dos feriados. Nomes, CNPJs e números são inventados; qualquer semelhança é coincidência.
 */
final class DadosDemo {

    private DadosDemo() {
    }
    static final String PREFIXO_CNPJ = "99";
    static final String PREFIXO_RODADA = "DEMO-";
    static final String FONTE = "Série fictícia para demonstração";

    /**
     * @param resposta chance de responder a cada campanha (os resorts, que concentram os leitos,
     *        sempre respondem; por isso as campanhas passam dos 70% de leitos para publicar)
     */
    record Estabelecimento(String nome, String tipo, String situacao, boolean ativo, String bairro,
            int uh, int uhPcd, int leitos, int maxLeitosPorUh, double resposta) {
    }

    static final List<Estabelecimento> ESTABELECIMENTOS = List.of(
            new Estabelecimento("Resort Vale das Águas", "RESORT", "REGULAR", true, "Jardim das Termas", 920, 18, 4600, 6, 1),
            new Estabelecimento("Resort Lagoa Serena", "RESORT", "REGULAR", true, "Jardim das Termas", 760, 15, 3420, 5, 1),
            new Estabelecimento("Resort Recanto Termal", "RESORT", "REGULAR", true, "Parque das Fontes", 610, 12, 3050, 6, 1),
            new Estabelecimento("Resort Sol Poente", "RESORT", "REGULAR", true, "Parque das Fontes", 450, 9, 2025, 5, 1),
            new Estabelecimento("Resort Jardim das Fontes", "RESORT", "REGULAR", true, "Jardim Europa", 380, 8, 1520, 4, 1),
            new Estabelecimento("Hotel Praça Nova", "HOTEL", "REGULAR", true, "Centro", 140, 4, 380, 3, 0.9),
            new Estabelecimento("Hotel Portal do Parque", "HOTEL", "REGULAR", true, "Parque das Fontes", 120, 3, 330, 3, 0.9),
            new Estabelecimento("Hotel Ipê Amarelo", "HOTEL", "REGULAR", true, "Centro", 85, 2, 230, 3, 0.85),
            new Estabelecimento("Hotel Rota Termal", "HOTEL", "REGULAR", true, "Jardim Europa", 70, 2, 190, 3, 0.85),
            new Estabelecimento("Hotel Estrela do Oeste", "HOTEL", "REGULAR", true, "Vila Santa Rosa", 60, 2, 160, 3, 0.8),
            new Estabelecimento("Hotel Avenida", "HOTEL", "REGULAR", true, "Centro", 48, 1, 120, 3, 0.8),
            new Estabelecimento("Hotel Bem-te-vi", "HOTEL", "REGULAR", true, "Jardim Glória", 40, 1, 100, 3, 0.75),
            new Estabelecimento("Pousada Canto do Sabiá", "POUSADA", "REGULAR", true, "Jardim Glória", 28, 1, 84, 4, 0.75),
            new Estabelecimento("Pousada Flor de Laranjeira", "POUSADA", "REGULAR", true, "Vila Santa Rosa", 22, 1, 66, 4, 0.7),
            new Estabelecimento("Pousada Recanto Verde", "POUSADA", "REGULAR", true, "Jardim Europa", 18, 1, 52, 4, 0.7),
            new Estabelecimento("Pousada Água Morna", "POUSADA", "REGULAR", true, "Parque das Fontes", 16, 1, 48, 4, 0.7),
            new Estabelecimento("Pousada Casa da Vó", "POUSADA", "REGULAR", true, "Centro", 12, 0, 34, 3, 0.65),
            new Estabelecimento("Pousada Céu Azul", "POUSADA", "REGULAR", true, "Jardim Glória", 10, 0, 28, 3, 0.6),
            new Estabelecimento("Hotel Fazenda Boa Esperança", "HOTEL_FAZENDA", "REGULAR", true, "Zona rural", 30, 1, 110, 5, 0.75),
            new Estabelecimento("Flat Residencial das Termas", "FLAT", "REGULAR", true, "Jardim das Termas", 64, 2, 192, 4, 0.75),
            new Estabelecimento("Hostel Mochila Leve", "HOSTEL", "REGULAR", true, "Centro", 12, 0, 60, 6, 0.6),
            new Estabelecimento("Pousada Recanto Sem Alvará", "POUSADA", "IRREGULAR", true, "Vila Santa Rosa", 9, 0, 27, 3, 0),
            new Estabelecimento("Hotel Antigo Viajante", "HOTEL", "REGULAR", false, "Centro", 35, 0, 90, 3, 0));

    /** Ampliação no meio do período, para a tela de capacidades ter histórico. */
    static final String ESTABELECIMENTO_AMPLIADO = "Hotel Portal do Parque";
    static final int UH_DEPOIS_DA_AMPLIACAO = 150;
    static final int LEITOS_DEPOIS_DA_AMPLIACAO = 420;

    /** Taxa de ocupação dos resorts por mês (de janeiro a dezembro): férias e verão em alta. */
    private static final double[] TAXA_POR_MES = { 79, 63, 49, 58, 53, 56, 81, 51, 55, 61, 53, 71 };

    /** Diferença de taxa de cada tipo em relação aos resorts. */
    private static final Map<String, Double> AJUSTE_DO_TIPO = Map.of(
            "RESORT", 0.0, "HOTEL", -9.0, "POUSADA", -15.0, "HOTEL_FAZENDA", -11.0, "FLAT", -6.0, "HOSTEL", -19.0);

    private static final Map<String, Double> DIARIA_DO_TIPO = Map.of(
            "RESORT", 980.0, "HOTEL", 390.0, "POUSADA", 270.0, "HOTEL_FAZENDA", 520.0, "FLAT", 310.0, "HOSTEL", 120.0);

    /** Feriados da demonstração e a taxa típica dos resorts em cada um. */
    enum Feriado {
        CARNAVAL(93), PASCOA(87), TIRADENTES(80), DIA_DO_TRABALHO(78), CORPUS_CHRISTI(84), FEFOL(88),
        INDEPENDENCIA(76), NOSSA_SENHORA_APARECIDA(83), NATAL_REVEILLON(91);

        final double taxa;

        Feriado(double taxa) {
            this.taxa = taxa;
        }

        /** Período do feriado no ano (para o Natal e Réveillon, o que começa em dezembro desse ano). */
        Periodo periodo(int ano) {
            LocalDate pascoa = domingoDePascoa(ano);
            return switch (this) {
                case CARNAVAL -> new Periodo(pascoa.minusDays(50), pascoa.minusDays(46));
                case PASCOA -> new Periodo(pascoa.minusDays(3), pascoa);
                case TIRADENTES -> new Periodo(LocalDate.of(ano, 4, 19), LocalDate.of(ano, 4, 21));
                case DIA_DO_TRABALHO -> new Periodo(LocalDate.of(ano, 5, 1), LocalDate.of(ano, 5, 3));
                case CORPUS_CHRISTI -> new Periodo(pascoa.plusDays(60), pascoa.plusDays(63));
                case FEFOL -> {
                    LocalDate inicio = LocalDate.of(ano, 8, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.SATURDAY));
                    yield new Periodo(inicio, inicio.plusDays(8));
                }
                case INDEPENDENCIA -> new Periodo(LocalDate.of(ano, 9, 5), LocalDate.of(ano, 9, 7));
                case NOSSA_SENHORA_APARECIDA -> new Periodo(LocalDate.of(ano, 10, 10), LocalDate.of(ano, 10, 12));
                case NATAL_REVEILLON -> new Periodo(LocalDate.of(ano, 12, 23), LocalDate.of(ano + 1, 1, 2));
            };
        }

        String nome(int ano) {
            return switch (this) {
                case CARNAVAL -> "Carnaval " + ano;
                case PASCOA -> "Páscoa " + ano;
                case TIRADENTES -> "Tiradentes " + ano;
                case DIA_DO_TRABALHO -> "1º de Maio " + ano;
                case CORPUS_CHRISTI -> "Corpus Christi " + ano;
                case FEFOL -> "FEFOL " + ano;
                case INDEPENDENCIA -> "7 de Setembro " + ano;
                case NOSSA_SENHORA_APARECIDA -> "12 de Outubro " + ano;
                case NATAL_REVEILLON -> "Natal e Réveillon " + ano + "/" + (ano + 1);
            };
        }
    }

    record Periodo(LocalDate inicio, LocalDate fim) {

        /** Número de diárias (noites) do período. */
        int diarias() {
            return (int) (fim.toEpochDay() - inicio.toEpochDay());
        }
    }

    /** Algoritmo de Meeus/Jones/Butcher para o domingo de Páscoa no calendário gregoriano. */
    static LocalDate domingoDePascoa(int ano) {
        int a = ano % 19, b = ano / 100, c = ano % 100, d = b / 4, e = b % 4;
        int f = (b + 8) / 25, g = (b - f + 1) / 3, h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31, dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }

    //  números das respostas

    /** Taxa de um estabelecimento num mês: sazonalidade, tipo, perfil do próprio estabelecimento e ruído. */
    static BigDecimal taxaMensal(Estabelecimento e, Month mes, Random sorteio) {
        return taxa(TAXA_POR_MES[mes.ordinal()], e, sorteio);
    }

    static BigDecimal taxaFeriado(Estabelecimento e, Feriado feriado, boolean expectativa, Random sorteio) {
        // a expectativa costuma ser um pouco mais otimista que o realizado
        return taxa(feriado.taxa + (expectativa ? 3 : 0), e, sorteio);
    }

    private static BigDecimal taxa(double base, Estabelecimento e, Random sorteio) {
        double perfil = (Math.floorMod(e.nome().hashCode(), 11) - 5); // de -5 a +5, fixo por estabelecimento
        double valor = base + AJUSTE_DO_TIPO.get(e.tipo()) + perfil + sorteio.nextGaussian() * 3;
        return arredondar(Math.max(8, Math.min(100, valor)), 2);
    }

    /** Diária média, ou null quando o estabelecimento não informa. */
    static BigDecimal diaria(Estabelecimento e, double fatorDaEpoca, Random sorteio) {
        if (!"RESORT".equals(e.tipo()) && sorteio.nextDouble() < 0.2) {
            return null;
        }
        double perfil = 1 + (Math.floorMod(e.nome().hashCode(), 21) - 10) / 100.0;
        double valor = DIARIA_DO_TIPO.get(e.tipo()) * fatorDaEpoca * perfil * (1 + sorteio.nextGaussian() * 0.03);
        return arredondar(valor, 2);
    }

    static double fatorDaDiaria(Month mes) {
        return switch (mes) {
            case JANUARY, JULY, DECEMBER -> 1.15;
            case MARCH, AUGUST, NOVEMBER -> 0.92;
            default -> 1.0;
        };
    }

    static double fatorDaDiaria(Feriado feriado) {
        return switch (feriado) {
            case CARNAVAL, NATAL_REVEILLON -> 1.35;
            case FEFOL, PASCOA -> 1.25;
            default -> 1.18;
        };
    }

    //  série histórica da cidade

    /** Leitos úteis do inventário da demonstração (UH × média entre 1 e a capacidade por UH). */
    static double leitosUteis() {
        return ESTABELECIMENTOS.stream()
                .filter(e -> e.ativo() && "REGULAR".equals(e.situacao()))
                .mapToDouble(e -> e.uh() * (1 + e.maxLeitosPorUh()) / 2.0)
                .sum();
    }

    /**
     * Valores da cidade num período anterior ao sistema. "Anos antes" deixa a série um pouco menor
     * no passado, como num destino que cresce.
     */
    record Historico(BigDecimal taxa, BigDecimal fluxo, BigDecimal diaria) {
    }

    static Historico historicoMensal(int ano, Month mes, int anosAntes, Random sorteio) {
        double taxa = TAXA_POR_MES[mes.ordinal()] - 3 - anosAntes * 1.5 + sorteio.nextGaussian() * 1.5;
        double diaria = 860 * fatorDaDiaria(mes) * Math.pow(0.94, anosAntes);
        return historico(taxa, mes.length(java.time.Year.isLeap(ano)), diaria, anosAntes);
    }

    static Historico historicoFeriado(Feriado feriado, int ano, boolean expectativa, int anosAntes, Random sorteio) {
        double taxa = feriado.taxa - 2 + (expectativa ? 3 : 0) - anosAntes * 1.5 + sorteio.nextGaussian() * 1.5;
        double diaria = 860 * fatorDaDiaria(feriado) * Math.pow(0.94, anosAntes);
        return historico(taxa, feriado.periodo(ano).diarias(), diaria, anosAntes);
    }

    private static Historico historico(double taxa, int noites, double diaria, int anosAntes) {
        taxa = Math.max(10, Math.min(98, taxa));
        // hospedados (leitos úteis ocupados por noite) + hospedagem caseira (~15%) + day use (15,15%)
        double fluxo = leitosUteis() * noites * taxa / 100 * 1.15 * 1.1515 * Math.pow(0.97, anosAntes);
        return new Historico(arredondar(taxa, 2), arredondar(fluxo, 0), arredondar(diaria, 2));
    }

    private static BigDecimal arredondar(double valor, int casas) {
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP);
    }
}
