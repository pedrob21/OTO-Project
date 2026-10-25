package OTO.calculo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * metodologia do OTO para taxa de ocupação, fluxo de turistas e receita, igual
 * às fórmulas que existem na planilha de tabulação (abas MH e TABULAÇÃO).
 *
 * <p>classe pura, sem Spring nem banco: recebe os dados de uma campanha e devolve
 * os indicadores. Usa {@code double} porque são estimativas estatísticas; o
 * arredondamento para duas casas acontece só na hora de gravar ou exibir.
 *
 * <p>taxas entram e saem em percentual (0 a 100).
 */
public final class CalculadoraOcupacao {

    private CalculadoraOcupacao() {
    }

    /** meio de hospedagem regular com a capacidade vigente. {@code taxaOcupacao} nula = não respondeu. */
    public record MeioHospedagem(long estabelecimentoId, String tipo, int qtdUh, int qtdLeitos, int maxLeitosPorUh,
            Double taxaOcupacao, Double diariaMedia) {

        /** leitos úteis: UH x média entre 1 e a capacidade máxima por UH. */
        public double leitosUteis() {
            return qtdUh * (1 + maxLeitosPorUh) / 2.0;
        }

        public boolean respondeu() {
            return taxaOcupacao != null;
        }
    }

    /** totais de hospedagem caseira (RHC). */
    public record HospedagemCaseira(int qtdUh, int qtdLeitos, int maxLeitosPorUh) {

        public static final HospedagemCaseira NENHUMA = new HospedagemCaseira(0, 0, 1);

        public double leitosUteis() {
            return qtdUh * (1 + maxLeitosPorUh) / 2.0;
        }
    }

    public record Parametros(double percentualDayUse, int diasPermanencia, int diasEmParques,
            double gastoMedioDiario, double gastoMedioIngresso) {
    }

    /** tipo de hospedagem, na ordem de exibição, e se entra no cálculo de receita. */
    public record Tipo(String codigo, boolean calculaReceita) {
    }

    /**
     * dados de uma campanha.
     *
     * @param diariasInformadas diária média por tipo digitada pela equipe (como na planilha); quando
     *        existe, substitui a média das respostas daquele tipo
     * @param diariaGeralInformada diária média geral digitada pela equipe; nula = média das respostas
     */
    public record Entrada(List<MeioHospedagem> meios, HospedagemCaseira rhc, int qtdDiarias, Parametros parametros,
            List<Tipo> tipos, Map<String, Double> diariasInformadas, Double diariaGeralInformada) {

        public Entrada {
            diariasInformadas = diariasInformadas == null ? Map.of() : Map.copyOf(diariasInformadas);
        }

        /** Sem diárias digitadas: todas saem das respostas. */
        public Entrada(List<MeioHospedagem> meios, HospedagemCaseira rhc, int qtdDiarias, Parametros parametros,
                List<Tipo> tipos) {
            this(meios, rhc, qtdDiarias, parametros, tipos, Map.of(), null);
        }
    }

    public record ResultadoTipo(String tipo, int qtdMh, int qtdMhRespondentes, int qtdUh, Double taxaOcupacao,
            Double diariaMedia, Double revpar, Double receitaEstimada) {
    }

    /** Faixa de classificação da taxa (Ruim a Excelente) e o percentual a partir do qual ela vale. */
    public record Faixa(String codigo, double percentualMinimo) {
    }

    public record Resultado(
            double taxaOcupacao,
            int qtdMh,
            int qtdMhRespondentes,
            int leitosTotal,
            int leitosRespondentes,
            double percentualLeitosRespondentes,
            double percentualMhRespondentes,
            double fluxoMh,
            double fluxoRhc,
            double fluxoDayUse,
            double fluxoTotal,
            double fluxoMedioDiario,
            Double diariaMedia,
            Double receitaHospedagem,
            double receitaSemHospedagem,
            List<ResultadoTipo> porTipo) {
    }

    public static Resultado calcular(Entrada entrada) {
        Objects.requireNonNull(entrada, "entrada");
        List<MeioHospedagem> meios = entrada.meios();
        HospedagemCaseira rhc = entrada.rhc() != null ? entrada.rhc() : HospedagemCaseira.NENHUMA;
        int diarias = entrada.qtdDiarias();
        Parametros p = entrada.parametros();
        if (diarias <= 0) {
            throw new IllegalArgumentException("A quantidade de diárias deve ser positiva.");
        }
        if (p.percentualDayUse() < 0 || p.percentualDayUse() >= 100) {
            throw new IllegalArgumentException("O percentual de day use deve estar entre 0 e 100.");
        }
        List<MeioHospedagem> respondentes = meios.stream().filter(MeioHospedagem::respondeu).toList();
        if (respondentes.isEmpty()) {
            throw new IllegalArgumentException("Nenhum meio de hospedagem respondeu à campanha.");
        }

        // Taxa geral: média das taxas dos respondentes ponderada pelas UH (TABULAÇÃO!J48).
        double taxa = mediaPonderadaPorUh(respondentes);
        double taxaFracao = taxa / 100.0;

        // Fluxo dos respondentes: leitos úteis x taxa x diárias de cada um (coluna M).
        double fluxoRespondentes = respondentes.stream()
                .mapToDouble(m -> m.leitosUteis() * (m.taxaOcupacao() / 100.0) * diarias)
                .sum();
        // Não respondentes: leitos úteis restantes x taxa geral x diárias (J86).
        double leitosUteisTotal = meios.stream().mapToDouble(MeioHospedagem::leitosUteis).sum();
        double leitosUteisRespondentes = respondentes.stream().mapToDouble(MeioHospedagem::leitosUteis).sum();
        double fluxoNaoRespondentes = (leitosUteisTotal - leitosUteisRespondentes) * taxaFracao * diarias;
        double fluxoMh = fluxoRespondentes + fluxoNaoRespondentes;

        // Hospedagem caseira com a taxa geral (J91).
        double fluxoRhc = rhc.leitosUteis() * taxaFracao * diarias;

        // Day use: os hospedados são (100 - day use)% do total de turistas por dia (J103, J96, J95).
        double hospedadosPorDia = (fluxoMh + fluxoRhc) / diarias;
        double totalPorDia = hospedadosPorDia / (1 - p.percentualDayUse() / 100.0);
        double fluxoDayUse = (totalPorDia - hospedadosPorDia) * diarias;

        double fluxoTotal = fluxoMh + fluxoRhc + fluxoDayUse;

        // Representatividade: leitos declarados dos respondentes sobre MH + RHC (K77) e MH respondentes (K75).
        int leitosRespondentes = respondentes.stream().mapToInt(MeioHospedagem::qtdLeitos).sum();
        int leitosTotal = meios.stream().mapToInt(MeioHospedagem::qtdLeitos).sum() + rhc.qtdLeitos();
        double percentualLeitos = leitosTotal == 0 ? 0 : 100.0 * leitosRespondentes / leitosTotal;
        double percentualMh = meios.isEmpty() ? 0 : 100.0 * respondentes.size() / meios.size();

        List<ResultadoTipo> porTipo = new ArrayList<>();
        Double receitaHospedagem = null;
        for (Tipo tipo : entrada.tipos()) {
            ResultadoTipo r = calcularTipo(tipo, meios, diarias, entrada.diariasInformadas().get(tipo.codigo()));
            if (r.qtdMh() == 0) {
                continue;
            }
            porTipo.add(r);
            if (r.receitaEstimada() != null) {
                receitaHospedagem = (receitaHospedagem == null ? 0 : receitaHospedagem) + r.receitaEstimada();
            }
        }

        // Diária média geral: a digitada pela equipe ou, sem ela, a das respostas ponderada pelos leitos.
        Double diariaGeral = entrada.diariaGeralInformada() != null
                ? entrada.diariaGeralInformada()
                : mediaDiariaPonderadaPorLeitos(respondentes);

        // Gasto fora da hospedagem: fluxo x (ingresso x dias em parques + gasto diário x dias de permanência).
        double receitaSemHospedagem = fluxoTotal
                * (p.gastoMedioIngresso() * p.diasEmParques() + p.gastoMedioDiario() * p.diasPermanencia());

        return new Resultado(taxa, meios.size(), respondentes.size(), leitosTotal, leitosRespondentes,
                percentualLeitos, percentualMh, fluxoMh, fluxoRhc, fluxoDayUse, fluxoTotal, fluxoTotal / diarias,
                diariaGeral, receitaHospedagem, receitaSemHospedagem, List.copyOf(porTipo));
    }

    private static ResultadoTipo calcularTipo(Tipo tipo, List<MeioHospedagem> meios, int diarias,
            Double diariaInformada) {
        List<MeioHospedagem> doTipo = meios.stream().filter(m -> tipo.codigo().equals(m.tipo())).toList();
        List<MeioHospedagem> respondentes = doTipo.stream().filter(MeioHospedagem::respondeu).toList();
        int qtdUh = doTipo.stream().mapToInt(MeioHospedagem::qtdUh).sum();

        Double taxa = respondentes.isEmpty() ? null : mediaPonderadaPorUh(respondentes);
        // Diária média: a digitada pela equipe ou, sem ela, a das respostas ponderada pelos leitos.
        Double diaria = diariaInformada != null ? diariaInformada : mediaDiariaPonderadaPorLeitos(respondentes);

        Double revpar = null;
        Double receita = null;
        if (tipo.calculaReceita() && taxa != null && diaria != null) {
            revpar = diaria * taxa / 100.0;
            receita = revpar * qtdUh * diarias;
        }
        return new ResultadoTipo(tipo.codigo(), doTipo.size(), respondentes.size(), qtdUh, taxa, diaria, revpar, receita);
    }

    /**
     * Quantos respondentes ficaram em cada faixa, como no quadro "% faixa de ocupação dos
     * MH respondentes" da tabulação. A faixa de uma taxa é a de maior mínimo que não passa
     * da taxa. O mapa segue a ordem das faixas recebidas e inclui as faixas sem ninguém.
     */
    public static Map<String, Integer> distribuirPorFaixa(List<MeioHospedagem> meios, List<Faixa> faixas) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        faixas.forEach(f -> contagem.put(f.codigo(), 0));
        for (MeioHospedagem m : meios) {
            if (m.respondeu()) {
                Faixa faixa = faixaDe(m.taxaOcupacao(), faixas);
                if (faixa != null) {
                    contagem.merge(faixa.codigo(), 1, Integer::sum);
                }
            }
        }
        return contagem;
    }

    /** Faixa de maior percentual mínimo que não passa da taxa; nula se a taxa ficar abaixo de todas. */
    public static Faixa faixaDe(double taxa, List<Faixa> faixas) {
        Faixa escolhida = null;
        for (Faixa f : faixas) {
            if (taxa >= f.percentualMinimo() && (escolhida == null || f.percentualMinimo() > escolhida.percentualMinimo())) {
                escolhida = f;
            }
        }
        return escolhida;
    }

    private static Double mediaDiariaPonderadaPorLeitos(List<MeioHospedagem> respondentes) {
        List<MeioHospedagem> comDiaria = respondentes.stream().filter(m -> m.diariaMedia() != null).toList();
        int leitosComDiaria = comDiaria.stream().mapToInt(MeioHospedagem::qtdLeitos).sum();
        return leitosComDiaria == 0 ? null
                : comDiaria.stream().mapToDouble(m -> m.diariaMedia() * m.qtdLeitos()).sum() / leitosComDiaria;
    }

    private static double mediaPonderadaPorUh(List<MeioHospedagem> respondentes) {
        double uh = respondentes.stream().mapToDouble(MeioHospedagem::qtdUh).sum();
        if (uh == 0) {
            throw new IllegalArgumentException("Os respondentes não têm UH cadastradas.");
        }
        return respondentes.stream().mapToDouble(m -> m.taxaOcupacao() * m.qtdUh()).sum() / uh;
    }
}
