package OTO.comum.auditoria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;

import tools.jackson.databind.json.JsonMapper;

class AuditoriaServiceTest {

    private final JdbcClient jdbc = mock(JdbcClient.class);
    private final StatementSpec comando = mock(StatementSpec.class);
    private final AuditoriaService auditoria = new AuditoriaService(jdbc, JsonMapper.builder().build());

    @Test
    void gravaUsuarioEntidadeAcaoEOsDadosEmJson() {
        when(jdbc.sql(contains("insert into auditoria"))).thenReturn(comando);
        when(comando.param(any())).thenReturn(comando);

        auditoria.registrar(1L, "usuario", 42L, "ATUALIZAR", Map.of("nome", "Ana"), Map.of("nome", "Bia"));

        InOrder ordem = inOrder(comando);
        ordem.verify(comando).param(1L);
        ordem.verify(comando).param("usuario");
        ordem.verify(comando).param("42");
        ordem.verify(comando).param("ATUALIZAR");
        ordem.verify(comando).param("{\"nome\":\"Ana\"}");
        ordem.verify(comando).param("{\"nome\":\"Bia\"}");
        ordem.verify(comando).update();
    }

    @Test
    void dadosAusentesFicamNulos() {
        when(jdbc.sql(contains("insert into auditoria"))).thenReturn(comando);
        when(comando.param(any())).thenReturn(comando);

        auditoria.registrar(1L, "indicador_historico", "importacao", "EXCLUIR", null, null);

        InOrder ordem = inOrder(comando);
        ordem.verify(comando).param("importacao");
        ordem.verify(comando).param("EXCLUIR");
        ordem.verify(comando, org.mockito.Mockito.times(2)).param((Object) null);
        ordem.verify(comando).update();
    }
}
