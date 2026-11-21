package OTO.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Leitura do JSON das respostas e comparação com os valores escritos nas funcionalidades (formato pt-BR). */
final class Json {

    /** Números como nas planilhas do OTO: 15.750,00 ou 60,00. */
    static final String NUMERO_PT = "-?(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,\\d+)?";

    private static final JsonMapper MAPEADOR = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();
    private static final Pattern PARTE_DO_CAMINHO = Pattern.compile("[^.\\[\\]]+|\\[(\\d+)\\]");

    private Json() {
    }

    static JsonNode ler(String texto) {
        return MAPEADOR.readTree(texto);
    }

    static String escrever(Object valor) {
        return MAPEADOR.writeValueAsString(valor);
    }

    /** Objeto JSON como mapa alterável, para reenviar com algumas mudanças. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> comoMapa(JsonNode objeto) {
        return MAPEADOR.treeToValue(objeto, LinkedHashMap.class);
    }

    /** "60,00" → 60.00; "15.750,00" → 15750.00. */
    static BigDecimal numero(String textoPt) {
        return new BigDecimal(textoPt.trim().replace(".", "").replace(",", "."));
    }

    /** Caminho no estilo "usuario.estabelecimentos[0].nomeFantasia". */
    static JsonNode caminho(JsonNode raiz, String caminho) {
        JsonNode atual = raiz;
        Matcher m = PARTE_DO_CAMINHO.matcher(caminho);
        while (m.find()) {
            assertNotNull(atual, "caminho inexistente: " + caminho);
            atual = m.group(1) != null ? atual.get(Integer.parseInt(m.group(1))) : atual.get(m.group());
        }
        assertNotNull(atual, "campo ausente na resposta: " + caminho);
        return atual;
    }

    /** Compara com o valor da funcionalidade: número pt-BR, "verdadeiro", "falso", "nulo" ou texto. */
    static void conferir(JsonNode atual, String esperado, String campo) {
        switch (esperado) {
            case "nulo" -> assertTrue(atual.isNull(), campo + ": " + atual);
            case "verdadeiro", "falso" -> {
                assertTrue(atual.isBoolean(), campo + ": " + atual);
                assertEquals("verdadeiro".equals(esperado), atual.booleanValue(), campo);
            }
            default -> {
                if (atual.isNumber() && esperado.matches(NUMERO_PT)) {
                    igual(atual, numero(esperado), campo);
                }
                else {
                    assertEquals(esperado, atual.asString(), campo);
                }
            }
        }
    }

    /** Igualdade numérica arredondando o valor da API para as casas decimais escritas na funcionalidade. */
    static void igual(JsonNode atual, BigDecimal esperado, String campo) {
        assertNotNull(atual, campo + ": ausente");
        assertFalse(atual.isNull(), campo + ": nulo, esperado " + esperado);
        int casas = Math.max(esperado.scale(), 0);
        BigDecimal valor = atual.decimalValue().setScale(casas, RoundingMode.HALF_UP);
        assertEquals(0, valor.compareTo(esperado), campo + ": " + atual + " em vez de " + esperado);
    }
}
