package OTO.aceitacao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import io.cucumber.spring.ScenarioScope;

/** O cliente da API guarda o estado de um cenário (tokens, última resposta) e é recriado a cada cenário. */
@TestConfiguration(proxyBeanMethods = false)
class ConfiguracaoDosPassos {

    @Bean
    @ScenarioScope
    Api api(JdbcTemplate jdbc, Environment ambiente) {
        return new Api(jdbc, ambiente);
    }
}
