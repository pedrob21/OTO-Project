package OTO.comum.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** verificação de saúde usada pela hospedagem (Render) para saber se a aplicação subiu. Sem login. */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
