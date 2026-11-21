package OTO.aceitacao;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Testes de aceitação (Plano de Testes, seção 5): as funcionalidades de
 * {@code src/test/resources/features} rodam contra a API real, com PostgreSQL de verdade.
 *
 * <p>Só as de fumaça: {@code ./gradlew test --tests OTO.aceitacao.CucumberTest -Ptags=@smoke}.
 * Relatório: {@code build/reports/cucumber/cucumber.html}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "OTO.aceitacao")
@ConfigurationParameter(key = "cucumber.plugin",
        value = "pretty, summary, html:build/reports/cucumber/cucumber.html, junit:build/test-results/cucumber/cucumber.xml")
@ConfigurationParameter(key = "cucumber.publish.quiet", value = "true")
class CucumberTest {
}
