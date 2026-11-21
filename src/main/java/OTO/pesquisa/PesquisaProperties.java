package OTO.pesquisa;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração das pesquisas.
 *
 * @param diasDeTolerancia dias depois do prazo de resposta em que o parceiro ainda consegue
 *        responder (padrão: 30; 0 = só até o dia do prazo). A equipe do OTO (ADMIN) não tem esse limite.
 */
@ConfigurationProperties("oto.pesquisa")
public record PesquisaProperties(@DefaultValue("30") int diasDeTolerancia) {

    public PesquisaProperties {
        if (diasDeTolerancia < 0) {
            throw new IllegalArgumentException("oto.pesquisa.dias-de-tolerancia não pode ser negativo.");
        }
    }
}
