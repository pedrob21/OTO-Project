package OTO;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe a aplicação inteira contra o PostgreSQL do compose.yaml (o Spring Boot inicia o
 * container sozinho, com o Docker aberto): confere as migrations, o mapeamento das
 * entidades (ddl-auto=validate), as consultas dos repositórios e a configuração.
 * Precisa do .env com as credenciais e o JWT_SEGREDO.
 */
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
class OtoProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
