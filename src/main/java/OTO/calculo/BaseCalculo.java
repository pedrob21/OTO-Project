package OTO.calculo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import OTO.estabelecimento.CapacidadeHospedagem;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.RhcResumo;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaDiaria;
import OTO.pesquisa.ParametroCalculo;
import OTO.pesquisa.RespostaOcupacao;
import OTO.referencia.TipoHospedagem;

/**
 * tudo o que entra no cálculo de uma campanha, lido do banco: os meios de hospedagem
 * regulares e ativos com a capacidade vigente, as respostas, o RHC e os parâmetros.
 *
 * @param dataReferencia último dia do período; vale o inventário, o RHC e os parâmetros vigentes nessa data
 * @param semCapacidade regulares e ativos sem capacidade cadastrada na data (ficam fora do cálculo)
 * @param respostasDesconsideradas respostas de quem não entra no cálculo (inativo, irregular ou sem capacidade)
 * @param rhc totais de hospedagem caseira; nulo se não houver cadastro
 * @param diariasInformadas diárias digitadas pela equipe para a campanha (por tipo e a geral)
 */
public record BaseCalculo(
        Campanha campanha,
        LocalDate dataReferencia,
        List<Participante> participantes,
        List<Estabelecimento> semCapacidade,
        List<RespostaOcupacao> respostasDesconsideradas,
        RhcResumo rhc,
        ParametroCalculo parametro,
        List<TipoHospedagem> tipos,
        List<CampanhaDiaria> diariasInformadas) {

    /** Meio de hospedagem que entra no cálculo. {@code resposta} nula = não respondeu. */
    public record Participante(Estabelecimento estabelecimento, CapacidadeHospedagem capacidade, RespostaOcupacao resposta) {

        public boolean respondeu() {
            return resposta != null;
        }
    }

    public int qtdRespondentes() {
        return (int) participantes.stream().filter(Participante::respondeu).count();
    }

    /** Leitos declarados dos meios de hospedagem do cálculo mais os do RHC (denominador da representatividade). */
    public int leitosTotal() {
        return participantes.stream().mapToInt(p -> p.capacidade().getQtdLeitos()).sum()
                + (rhc == null ? 0 : rhc.getQtdLeitos());
    }

    public int leitosRespondentes() {
        return participantes.stream().filter(Participante::respondeu).mapToInt(p -> p.capacidade().getQtdLeitos()).sum();
    }

    /** Mesmo cálculo da calculadora, disponível mesmo antes da primeira resposta. */
    public double percentualLeitosRespondentes() {
        int total = leitosTotal();
        return total == 0 ? 0 : 100.0 * leitosRespondentes() / total;
    }

    public CalculadoraOcupacao.Entrada paraEntrada() {
        List<CalculadoraOcupacao.MeioHospedagem> meios = participantes.stream()
                .map(p -> new CalculadoraOcupacao.MeioHospedagem(
                        p.estabelecimento().getId(),
                        p.estabelecimento().getTipoHospedagemCodigo(),
                        p.capacidade().getQtdUh(),
                        p.capacidade().getQtdLeitos(),
                        p.capacidade().getMaxLeitosPorUh(),
                        p.respondeu() ? p.resposta().getTaxaOcupacao().doubleValue() : null,
                        p.respondeu() ? Numeros.paraDouble(p.resposta().getDiariaMedia()) : null))
                .toList();
        CalculadoraOcupacao.HospedagemCaseira caseira = rhc == null
                ? CalculadoraOcupacao.HospedagemCaseira.NENHUMA
                : new CalculadoraOcupacao.HospedagemCaseira(rhc.getQtdUh(), rhc.getQtdLeitos(), rhc.getMaxLeitosPorUh());
        CalculadoraOcupacao.Parametros parametros = new CalculadoraOcupacao.Parametros(
                parametro.getPercentualDayUse().doubleValue(),
                parametro.getDiasPermanenciaMedia(),
                parametro.getDiasEmParques(),
                parametro.getGastoMedioDiario().doubleValue(),
                parametro.getGastoMedioIngresso().doubleValue());
        List<CalculadoraOcupacao.Tipo> listaTipos = tipos.stream()
                .map(t -> new CalculadoraOcupacao.Tipo(t.getCodigo(), t.isCalculaReceita()))
                .toList();
        Map<String, Double> diariasPorTipo = new HashMap<>();
        Double diariaGeral = null;
        for (CampanhaDiaria d : diariasInformadas) {
            if (d.isGeral()) {
                diariaGeral = d.getDiariaMedia().doubleValue();
            }
            else {
                diariasPorTipo.put(d.getTipoHospedagemCodigo(), d.getDiariaMedia().doubleValue());
            }
        }
        return new CalculadoraOcupacao.Entrada(meios, caseira, campanha.getQtdDiarias(), parametros, listaTipos,
                diariasPorTipo, diariaGeral);
    }
}
