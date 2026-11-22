package OTO;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL 17 compartilhado por todos os testes de repositório: um só container por execução da
 * JVM, iniciado na primeira vez que é usado. Precisa do Docker.
 */
@TestConfiguration(proxyBeanMethods = false)
class BancoDeTesteRepositorio {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return POSTGRES;
    }
}
