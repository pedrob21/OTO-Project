package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import OTO.calculo.ResultadoDtos.FaixaQuantidade;
import OTO.calculo.ResultadoDtos.FaixaRef;
import OTO.calculo.ResultadoDtos.ResultadoResponse;
import OTO.calculo.ResultadoDtos.ResultadoTipoResponse;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.TipoPeriodo;

class ResultadoCsvTest {

    private static ResultadoResponse resultado(String nomeCampanha, FaixaRef faixa, BigDecimal diaria) {
        CampanhaResponse campanha = new CampanhaResponse(10L, 1L, nomeCampanha, Modalidade.CONSOLIDADA, TipoPeriodo.MENSAL,
                null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), 28, true, Instant.now());
        return new ResultadoResponse(campanha, true, new BigDecimal("60.5"), faixa, 3, 2, new BigDecimal("66.67"), 117, 80,
                new BigDecimal("68.38"), new BigDecimal("1540"), new BigDecimal("134.4"), new BigDecimal("300"),
                new BigDecimal("1974.4"), new BigDecimal("70.51"), diaria, new BigDecimal("12345.678"),
                new BigDecimal("2389024"), 1L, Instant.now(),
                List.of(new ResultadoTipoResponse("HOTEL", "Hotel", 2, 2, 30, new BigDecimal("60"), new BigDecimal("437.5"),
                                new BigDecimal("262.5"), new BigDecimal("220500"), false),
                        new ResultadoTipoResponse("POUSADA", "Pousada; \"Chalés\"", 1, 0, 10, null, null, null, null, true)),
                List.of(new FaixaQuantidade("OTIMA", "Ótima", new BigDecimal("61"), 1, new BigDecimal("50"))));
    }

    private static String gerar(ResultadoResponse r) {
        return new String(ResultadoCsv.gerar(r), StandardCharsets.UTF_8);
    }

    @Test
    void comecaComBomEUsaPontoEVirgulaEVirgulaDecimal() {
        String csv = gerar(resultado("Fevereiro 2026", new FaixaRef("BOA", "Boa"), new BigDecimal("437.5")));

        assertThat(csv).startsWith("﻿Campanha;Fevereiro 2026\r\n");
        assertThat(csv).contains("Modalidade;CONSOLIDADA\r\n");
        assertThat(csv).contains("Período;01/02/2026 a 28/02/2026\r\n");
        assertThat(csv).contains("Diárias;28\r\n");
        assertThat(csv).contains("Taxa de ocupação (%);60,50\r\n");
        assertThat(csv).contains("Faixa;Boa\r\n");
        assertThat(csv).contains("Leitos (MH + RHC);117\r\n");
        assertThat(csv).contains("Diária média (R$);437,50\r\n");
        assertThat(csv).contains("Receita estimada da hospedagem (R$);12345,68\r\n");
        assertThat(csv).contains("Hotel;2;2;30;60,00;437,50;262,50;220500,00\r\n");
        assertThat(csv).contains("Ótima;1;50,00\r\n");
        assertThat(csv).endsWith("\r\n");
    }

    @Test
    void valoresAusentesFicamVaziosETextoComSeparadorVaiEntreAspas() {
        String csv = gerar(resultado("Carnaval; Expectativa", null, null));

        assertThat(csv).startsWith("﻿Campanha;\"Carnaval; Expectativa\"\r\n");
        assertThat(csv).contains("Faixa;\r\n");
        assertThat(csv).contains("Diária média (R$);\r\n");
        assertThat(csv).contains("\"Pousada; \"\"Chalés\"\"\";1;0;10;;;;\r\n");
    }
}
