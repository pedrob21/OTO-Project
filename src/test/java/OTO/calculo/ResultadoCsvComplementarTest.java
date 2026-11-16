package OTO.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import OTO.calculo.ResultadoDtos.ResultadoResponse;
import OTO.calculo.ResultadoDtos.ResultadoTipoResponse;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.RodadaDtos.CampanhaResponse;
import OTO.pesquisa.TipoPeriodo;

/** Escape de texto do CSV: nulo, aspas sem separador e quebra de linha. */
class ResultadoCsvComplementarTest {

    private static ResultadoResponse resultado(String nomeCampanha, String... nomesTipos) {
        CampanhaResponse campanha = new CampanhaResponse(10L, 1L, nomeCampanha, Modalidade.CONSOLIDADA, TipoPeriodo.MENSAL,
                null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), 28, true, Instant.now());
        List<ResultadoTipoResponse> tipos = java.util.Arrays.stream(nomesTipos)
                .map(nome -> new ResultadoTipoResponse("T", nome, 1, 1, 10, null, null, null, null, false))
                .toList();
        return new ResultadoResponse(campanha, true, null, null, 1, 1, null, 10, 10, null, null, null, null, null, null,
                null, null, null, 1L, Instant.now(), tipos, List.of());
    }

    private static String gerar(ResultadoResponse r) {
        return new String(ResultadoCsv.gerar(r), StandardCharsets.UTF_8);
    }

    @Test
    void textoNuloFicaVazio() {
        assertThat(gerar(resultado(null))).startsWith("﻿Campanha;\r\n");
    }

    @Test
    void textoComAspasOuQuebraDeLinhaVaiEntreAspas() {
        String csv = gerar(resultado("Fevereiro", "Pousada \"Chalés\"", "Hotel\nFazenda"));

        assertThat(csv).contains("\"Pousada \"\"Chalés\"\"\";1;1;10;;;;\r\n");
        assertThat(csv).contains("\"Hotel\nFazenda\";1;1;10;;;;\r\n");
    }
}
