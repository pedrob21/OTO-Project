package OTO.comum.auditoria;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * grava quem alterou o quê na tabela auditoria. Roda na mesma transação da
 * operação: se ela falhar, o registro também é desfeito (rollback).
 */
@Service
public class AuditoriaService {

    private static final String INSERT = """
            insert into auditoria (usuario_id, entidade, entidade_id, acao, dados_antes, dados_depois)
            values (?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb))
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public AuditoriaService(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void registrar(Long usuarioId, String entidade, Object entidadeId, String acao, Object antes, Object depois) {
        jdbc.sql(INSERT)
                .param(usuarioId)
                .param(entidade)
                .param(String.valueOf(entidadeId))
                .param(acao)
                .param(paraJson(antes))
                .param(paraJson(depois))
                .update();
    }

    private String paraJson(Object valor) {
        return valor == null ? null : json.writeValueAsString(valor);
    }
}
