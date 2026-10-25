package OTO.calculo;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import OTO.calculo.ResultadoDtos.FaixaQuantidade;
import OTO.calculo.ResultadoDtos.ResultadoResponse;
import OTO.calculo.ResultadoDtos.ResultadoTipoResponse;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;

/**
 * resultado em CSV para abrir no Excel em português: separador ";", vírgula decimal e
 * UTF-8 com BOM (sem o BOM o Excel troca os acentos).
 */
final class ResultadoCsv {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ResultadoCsv() {
    }

    static byte[] gerar(ResultadoResponse r) {
        CampanhaResponse c = r.campanha();
        StringBuilder csv = new StringBuilder("﻿");
        linha(csv, "Campanha", c.nome());
        linha(csv, "Modalidade", c.modalidade().name());
        linha(csv, "Período", DATA.format(c.periodoInicio()) + " a " + DATA.format(c.periodoFim()));
        linha(csv, "Diárias", String.valueOf(c.qtdDiarias()));
        csv.append("\r\n");

        linha(csv, "Indicador", "Valor");
        linha(csv, "Taxa de ocupação (%)", numero(r.taxaOcupacao()));
        linha(csv, "Faixa", r.faixa() == null ? "" : r.faixa().nome());
        linha(csv, "Meios de hospedagem", String.valueOf(r.qtdMh()));
        linha(csv, "Meios de hospedagem respondentes", String.valueOf(r.qtdMhRespondentes()));
        linha(csv, "Respondentes (%)", numero(r.percentualMhRespondentes()));
        linha(csv, "Leitos (MH + RHC)", String.valueOf(r.leitosTotal()));
        linha(csv, "Leitos respondentes", String.valueOf(r.leitosRespondentes()));
        linha(csv, "Leitos respondentes (%)", numero(r.percentualLeitosRespondentes()));
        linha(csv, "Fluxo em meios de hospedagem (pernoites)", numero(r.fluxoMh()));
        linha(csv, "Fluxo em hospedagem caseira (pernoites)", numero(r.fluxoRhc()));
        linha(csv, "Fluxo de day use", numero(r.fluxoDayUse()));
        linha(csv, "Fluxo total de turistas", numero(r.fluxoTotal()));
        linha(csv, "Fluxo médio por dia", numero(r.fluxoMedioDiario()));
        linha(csv, "Diária média (R$)", numero(r.diariaMedia()));
        linha(csv, "Receita estimada da hospedagem (R$)", numero(r.receitaHospedagem()));
        linha(csv, "Receita estimada fora da hospedagem (R$)", numero(r.receitaSemHospedagem()));
        csv.append("\r\n");

        linha(csv, "Tipo", "Meios de hospedagem", "Respondentes", "UH", "Taxa de ocupação (%)", "Diária média (R$)",
                "RevPAR (R$)", "Receita estimada (R$)");
        for (ResultadoTipoResponse t : r.porTipo()) {
            linha(csv, t.nome(), String.valueOf(t.qtdMh()), String.valueOf(t.qtdMhRespondentes()),
                    String.valueOf(t.qtdUh()), numero(t.taxaOcupacao()), numero(t.diariaMedia()), numero(t.revpar()),
                    numero(t.receitaEstimada()));
        }
        csv.append("\r\n");

        linha(csv, "Faixa de ocupação", "Respondentes", "Percentual (%)");
        for (FaixaQuantidade f : r.distribuicaoFaixas()) {
            linha(csv, f.nome(), String.valueOf(f.qtdMh()), numero(f.percentual()));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void linha(StringBuilder csv, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(texto(campos[i]));
        }
        csv.append("\r\n");
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return '"' + valor.replace("\"", "\"\"") + '"';
        }
        return valor;
    }

    private static String numero(BigDecimal valor) {
        return valor == null ? "" : String.format(PT_BR, "%.2f", valor);
    }
}
