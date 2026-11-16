package OTO;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

/** O main só delega ao Spring; sem subir o contexto (que precisa do banco). */
class OtoProjectApplicationTest {

    @Test
    void mainSobeAAplicacaoComOsArgumentos() {
        String[] args = { "--server.port=0" };
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            OtoProjectApplication.main(args);

            spring.verify(() -> SpringApplication.run(OtoProjectApplication.class, args));
        }
    }
}
