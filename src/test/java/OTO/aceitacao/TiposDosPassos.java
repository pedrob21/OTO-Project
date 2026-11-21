package OTO.aceitacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.cucumber.java.ParameterType;

/** Tipos usados nas frases: quem chama a API, método HTTP, percentual e valores no formato brasileiro. */
public class TiposDosPassos {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Usuários da massa por papel: ADMIN, PARCEIRO A (Hotéis A e B), PARCEIRO C (Pousada C), VISUALIZADOR. */
    @ParameterType("ADMIN|PARCEIRO A|PARCEIRO C|VISUALIZADOR|ANÔNIMO")
    public String ator(String ator) {
        return ator;
    }

    @ParameterType("GET|POST|PUT|DELETE")
    public String metodo(String metodo) {
        return metodo;
    }

    /** 68,38% → 68.38 */
    @ParameterType(Json.NUMERO_PT + "%")
    public BigDecimal percentual(String texto) {
        return Json.numero(texto.substring(0, texto.length() - 1));
    }

    /** 15.750,00 → 15750.00 */
    @ParameterType(Json.NUMERO_PT)
    public BigDecimal valor(String texto) {
        return Json.numero(texto);
    }

    /** 01/06/2026 */
    @ParameterType("\\d{2}/\\d{2}/\\d{4}")
    public LocalDate data(String texto) {
        return LocalDate.parse(texto, DATA_BR);
    }
}
