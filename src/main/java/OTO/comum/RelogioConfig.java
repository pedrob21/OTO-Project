package OTO.comum;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio da aplicação no fuso do OTO. Datas como "hoje" e o fim do prazo das pesquisas
 * são contadas no horário de Olímpia, e não em UTC (senão o dia virava às 21h).
 * Os testes trocam este relógio por um fixo.
 */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    @Bean
    public Clock relogio(@Value("${oto.fuso-horario:America/Sao_Paulo}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
