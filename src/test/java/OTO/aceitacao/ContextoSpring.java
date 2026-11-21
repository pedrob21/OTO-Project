package OTO.aceitacao;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Sobe a aplicação inteira numa porta aleatória, com um PostgreSQL 17 descartável
 * (Testcontainers). O contexto é criado uma vez e vale para todos os cenários.
 * Segredos só de teste: nada vem do .env.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:postgresql://localhost/substituido-pelo-testcontainers",
        "spring.datasource.username=oto",
        "spring.datasource.password=oto",
        "spring.flyway.clean-disabled=false",
        "spring.docker.compose.enabled=false",
        "oto.jwt.segredo=segredo-so-dos-testes-de-aceitacao-0123456789abcdef",
        "oto.admin-inicial.email=",
        "logging.level.root=WARN"
})
@Import({ BancoDeTeste.class, ConfiguracaoDosPassos.class })
public class ContextoSpring {
}
