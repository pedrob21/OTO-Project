package OTO.aceitacao;

import org.flywaydb.core.Flyway;

import io.cucumber.java.Before;

/** Cada cenário começa com o banco recém-migrado: o que um cenário grava não vaza para o outro. */
public class Ganchos {

    private final Flyway flyway;

    public Ganchos(Flyway flyway) {
        this.flyway = flyway;
    }

    @Before(order = 0)
    public void limparBanco() {
        flyway.clean();
        flyway.migrate();
    }
}
