package OTO.aceitacao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL 17 em container, ligado à aplicação pelo {@code @ServiceConnection}. Precisa do Docker. */
@TestConfiguration(proxyBeanMethods = false)
class BancoDeTeste {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
