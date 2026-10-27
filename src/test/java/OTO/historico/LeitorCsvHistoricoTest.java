package OTO.historico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import OTO.historico.LeitorCsvHistorico.FalhaCsv;
import OTO.historico.LeitorCsvHistorico.Leitura;
import OTO.historico.LeitorCsvHistorico.LinhaCsv;
import OTO.pesquisa.Modalidade;

class LeitorCsvHistoricoTest {

    private static Leitura ler(String texto) {
        return LeitorCsvHistorico.ler(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void leOFormatoDoExcelEmPortugues() {
        Leitura leitura = ler("""
                indicador;modalidade;ano;mes;feriado;tipo;valor;observacao
                TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45;
                FLUXO_TURISTAS;CONSOLIDADA;2024;;CARNAVAL;;98.512;
                DIARIA_MEDIA;CONSOLIDADA;2024;7;;HOTEL;412,90;Booking
                """);

        assertThat(leitura.falhas()).isEmpty();
        assertThat(leitura.linhas()).containsExactly(
                new LinhaCsv(2, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, null,
                        new BigDecimal("63.45"), null),
                new LinhaCsv(3, Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA, 2024, null, "CARNAVAL", null,
                        new BigDecimal("98512"), null),
                new LinhaCsv(4, Indicador.DIARIA_MEDIA, Modalidade.CONSOLIDADA, 2024, 7, null, "HOTEL",
                        new BigDecimal("412.90"), "Booking"));
    }

    @Test
    void aceitaNomesComAcentoEspacosEMinusculas() {
        LinhaCsv linha = ler("Taxa ocupação; consolidada ;2024;2; ;hotel;70%;").linhas().get(0);
        assertThat(linha.indicador()).isEqualTo(Indicador.TAXA_OCUPACAO);
        assertThat(linha.modalidade()).isEqualTo(Modalidade.CONSOLIDADA);
        assertThat(linha.feriadoCodigo()).isNull();
        assertThat(linha.tipoHospedagemCodigo()).isEqualTo("HOTEL");
        assertThat(linha.valor()).isEqualByComparingTo("70");
    }

    @Test
    void ignoraBomLinhasEmBrancoEUsaONumeroDaLinhaDoArquivo() {
        Leitura leitura = ler("﻿indicador;modalidade;ano;mes;feriado;tipo;valor;observacao\r\n\r\n;;;;;;;\r\n"
                + "TAXA_OCUPACAO;EXPECTATIVA;2023;12;;;50;\r\n");
        assertThat(leitura.linhas()).hasSize(1);
        assertThat(leitura.linhas().get(0).numero()).isEqualTo(4);
        assertThat(leitura.linhas().get(0).modalidade()).isEqualTo(Modalidade.EXPECTATIVA);
    }

    @Test
    void semCabecalhoAPrimeiraLinhaJaEhDado() {
        assertThat(ler("TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45").linhas()).hasSize(1);
    }

    @Test
    void cabecalhoNoMeioDoArquivoEhErro() {
        Leitura leitura = ler("TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45\nindicador;modalidade;ano;mes;feriado;tipo;valor\n");
        assertThat(leitura.falhas()).extracting(FalhaCsv::numero).containsExactly(2);
    }

    @Test
    void observacaoFicaComOQueSobraETiraAspas() {
        LinhaCsv linha = ler("TAXA_OCUPACAO;CONSOLIDADA;2024;1;\"\";;\"63,45\";\"Fonte; \"\"oficial\"\"\";extra;;").linhas().get(0);
        assertThat(linha.valor()).isEqualByComparingTo("63.45");
        assertThat(linha.observacao()).isEqualTo("\"Fonte; \"\"oficial\"\"\";extra");
    }

    @Test
    void semColunaDeObservacaoElaFicaNula() {
        assertThat(ler("TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45").linhas().get(0).observacao()).isNull();
    }

    @Test
    void linhasComProblemaViramFalhasComMensagem() {
        Leitura leitura = ler("""
                TAXA;CONSOLIDADA;2024;1;;;1
                TAXA_OCUPACAO;REALIZADA;2024;1;;;1
                TAXA_OCUPACAO;CONSOLIDADA;dois mil;1;;;1
                TAXA_OCUPACAO;CONSOLIDADA;2024;jan;;;1
                TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;
                TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;abc
                TAXA_OCUPACAO;CONSOLIDADA
                """);

        assertThat(leitura.linhas()).isEmpty();
        assertThat(leitura.falhas()).extracting(FalhaCsv::mensagem).containsExactly(
                "indicador inválido: \"TAXA\" (use TAXA_OCUPACAO, FLUXO_TURISTAS, DIARIA_MEDIA)",
                "modalidade inválido: \"REALIZADA\" (use CONSOLIDADA, EXPECTATIVA)",
                "ano inválido: \"dois mil\"",
                "mês inválido: \"jan\"",
                "valor vazio",
                "valor inválido: \"abc\"",
                "esperadas as colunas indicador;modalidade;ano;mes;feriado;tipo;valor;observacao");
        assertThat(leitura.falhas()).extracting(FalhaCsv::numero).containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    void decimalEntendeVirgulaPontoDeMilharEMoeda() {
        assertThat(LeitorCsvHistorico.decimal("63,45")).isEqualByComparingTo("63.45");
        assertThat(LeitorCsvHistorico.decimal("1.234,56")).isEqualByComparingTo("1234.56");
        assertThat(LeitorCsvHistorico.decimal("98.512")).isEqualByComparingTo("98512");
        assertThat(LeitorCsvHistorico.decimal("1.234.567")).isEqualByComparingTo("1234567");
        assertThat(LeitorCsvHistorico.decimal("63.4")).isEqualByComparingTo("63.4");
        assertThat(LeitorCsvHistorico.decimal("63.45")).isEqualByComparingTo("63.45");
        assertThat(LeitorCsvHistorico.decimal(" R$ 412,90 ")).isEqualByComparingTo("412.90");
        assertThat(LeitorCsvHistorico.decimal("R$ 1.000,00")).isEqualByComparingTo("1000");
        assertThat(LeitorCsvHistorico.decimal("70%")).isEqualByComparingTo("70");
        assertThatThrownBy(() -> LeitorCsvHistorico.decimal(" % ")).hasMessage("valor vazio");
        assertThatThrownBy(() -> LeitorCsvHistorico.decimal("1,2,3")).hasMessage("valor inválido: \"1,2,3\"");
    }

    @Test
    void decodificaUtf8EWindows1252() {
        assertThat(LeitorCsvHistorico.decodificar("﻿ocupação".getBytes(StandardCharsets.UTF_8))).isEqualTo("ocupação");
        assertThat(LeitorCsvHistorico.decodificar("ocupação".getBytes(Charset.forName("windows-1252")))).isEqualTo("ocupação");
    }

    @Test
    void arquivoDoExcelEmWindows1252() {
        byte[] conteudo = "Diária média;Consolidada;2024;3;;;400,00;Pesquisa própria\r\n".getBytes(Charset.forName("windows-1252"));
        LinhaCsv linha = LeitorCsvHistorico.ler(conteudo).linhas().get(0);
        assertThat(linha.indicador()).isEqualTo(Indicador.DIARIA_MEDIA);
        assertThat(linha.observacao()).isEqualTo("Pesquisa própria");
    }
}
