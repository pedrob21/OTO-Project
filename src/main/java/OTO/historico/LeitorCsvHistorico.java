package OTO.historico;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import OTO.pesquisa.Modalidade;

/**
 * Lê o CSV da série histórica, no formato que o Excel em português salva:
 *
 * <pre>
 * indicador;modalidade;ano;mes;feriado;tipo;valor;observacao
 * TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45;
 * FLUXO_TURISTAS;CONSOLIDADA;2024;;CARNAVAL;;98.512;
 * DIARIA_MEDIA;CONSOLIDADA;2024;7;;HOTEL;412,90;
 * </pre>
 *
 * A primeira linha pode ser o cabeçalho. Aceita UTF-8 (com ou sem BOM) e o
 * Windows-1252 do "CSV (separado por vírgulas)" do Excel. No valor, a vírgula é o
 * separador decimal e o ponto, o de milhar ("98.512" = 98512; "63.4" também vale 63,4).
 */
final class LeitorCsvHistorico {

    private static final int COLUNAS = 8;

    /** Linha do arquivo já convertida, ainda sem as validações de domínio. */
    record LinhaCsv(int numero, Indicador indicador, Modalidade modalidade, int ano, Integer mes, String feriadoCodigo,
            String tipoHospedagemCodigo, BigDecimal valor, String observacao) {
    }

    /** Linha que não pôde ser convertida. */
    record FalhaCsv(int numero, String mensagem) {
    }

    record Leitura(List<LinhaCsv> linhas, List<FalhaCsv> falhas) {
    }

    private LeitorCsvHistorico() {
    }

    static Leitura ler(byte[] conteudo) {
        String[] linhas = decodificar(conteudo).split("\r\n|\n|\r", -1);
        List<LinhaCsv> lidas = new ArrayList<>();
        List<FalhaCsv> falhas = new ArrayList<>();
        boolean primeira = true;
        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i];
            if (linha.isBlank() || linha.replace(";", "").isBlank()) {
                continue;
            }
            String[] campos = dividir(linha);
            if (primeira && normalizar(campos[0]).equals("INDICADOR")) {
                primeira = false;
                continue;
            }
            primeira = false;
            try {
                lidas.add(converter(i + 1, campos));
            }
            catch (IllegalArgumentException e) {
                falhas.add(new FalhaCsv(i + 1, e.getMessage()));
            }
        }
        return new Leitura(lidas, falhas);
    }

    private static LinhaCsv converter(int numero, String[] campos) {
        if (campos.length < COLUNAS - 1) {
            throw new IllegalArgumentException("esperadas as colunas indicador;modalidade;ano;mes;feriado;tipo;valor;observacao");
        }
        Indicador indicador = enumDe(Indicador.class, campos[0], "indicador");
        Modalidade modalidade = enumDe(Modalidade.class, campos[1], "modalidade");
        int ano = inteiro(campos[2], "ano");
        Integer mes = vazio(campos[3]) ? null : inteiro(campos[3], "mês");
        String feriado = vazio(campos[4]) ? null : normalizar(campos[4]);
        String tipo = vazio(campos[5]) ? null : normalizar(campos[5]);
        BigDecimal valor = decimal(campos[6]);
        String observacao = campos.length < COLUNAS ? null : campos[7].replaceAll(";+$", "").trim();
        return new LinhaCsv(numero, indicador, modalidade, ano, mes, feriado, tipo, valor,
                observacao == null || observacao.isEmpty() ? null : observacao);
    }

    /** Separa por ";". A partir da 8ª coluna tudo vai para a observação. Tira aspas do Excel. */
    private static String[] dividir(String linha) {
        String[] campos = linha.split(";", COLUNAS);
        for (int i = 0; i < campos.length; i++) {
            String campo = campos[i].trim();
            if (campo.length() >= 2 && campo.startsWith("\"") && campo.endsWith("\"")) {
                campo = campo.substring(1, campo.length() - 1).replace("\"\"", "\"");
            }
            campos[i] = campo;
        }
        return campos;
    }

    private static <E extends Enum<E>> E enumDe(Class<E> tipo, String texto, String nome) {
        try {
            return Enum.valueOf(tipo, normalizar(texto));
        }
        catch (IllegalArgumentException e) {
            List<String> validos = new ArrayList<>();
            for (E valor : tipo.getEnumConstants()) {
                validos.add(valor.name());
            }
            throw new IllegalArgumentException(nome + " inválido: \"" + texto.trim() + "\" (use " + String.join(", ", validos) + ")");
        }
    }

    private static int inteiro(String texto, String nome) {
        try {
            return Integer.parseInt(texto.trim());
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(nome + " inválido: \"" + texto.trim() + "\"");
        }
    }

    static BigDecimal decimal(String texto) {
        String s = texto.trim().replace("%", "").replace("R$", "").replace(" ", "").replace(" ", "");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("valor vazio");
        }
        if (s.contains(",")) {
            s = s.replace(".", "").replace(",", ".");
        }
        else if (s.indexOf('.') != s.lastIndexOf('.')) {
            s = s.replace(".", "");
        }
        else if (s.contains(".") && s.length() - s.indexOf('.') - 1 == 3) {
            s = s.replace(".", "");
        }
        try {
            return new BigDecimal(s);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido: \"" + texto.trim() + "\"");
        }
    }

    /** Maiúsculas, sem acentos e com "_" no lugar de espaços e hífens ("Taxa ocupação" → TAXA_OCUPACAO). */
    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    static String decodificar(byte[] conteudo) {
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(conteudo))
                    .toString();
        }
        catch (CharacterCodingException e) {
            texto = new String(conteudo, Charset.forName("windows-1252"));
        }
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }
}
