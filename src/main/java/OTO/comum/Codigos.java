package OTO.comum;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

import OTO.comum.excecao.RegraNegocioException;

/**
 * códigos internos dos cadastros (ex.: AREA_DE_LAZER): letras sem acento, números e "_".
 * Quando a equipe não informa o código, ele é gerado a partir do nome.
 */
public final class Codigos {

    private Codigos() {
    }

    /** "Área de lazer" vira "AREA_DE_LAZER", com no máximo {@code tamanho} caracteres. */
    public static String deNome(String nome, int tamanho) {
        String semAcento = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String codigo = semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        return cortar(codigo, tamanho);
    }

    /** Código gerado do nome que ainda não existe: acrescenta _2, _3... quando preciso. */
    public static String livre(String nome, int tamanho, Predicate<String> existe) {
        String base = deNome(nome, tamanho);
        if (base.isEmpty()) {
            throw new RegraNegocioException("Informe o código: não foi possível gerá-lo a partir do nome.");
        }
        String codigo = base;
        for (int n = 2; existe.test(codigo); n++) {
            String sufixo = "_" + n;
            codigo = cortar(base, tamanho - sufixo.length()) + sufixo;
        }
        return codigo;
    }

    /** Código digitado pela equipe: passa para maiúsculas e confere o formato. */
    public static String informado(String codigo, int tamanho) {
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
        if (!normalizado.matches("[A-Z0-9_]+") || normalizado.length() > tamanho) {
            throw new RegraNegocioException("Código inválido: use letras sem acento, números e _ (até " + tamanho
                    + " caracteres).");
        }
        return normalizado;
    }

    public static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String cortar(String codigo, int tamanho) {
        return codigo.length() <= tamanho ? codigo : codigo.substring(0, tamanho).replaceAll("_+$", "");
    }
}
