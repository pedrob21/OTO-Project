-- =============================================================================
-- V10 - Diária média digitada pela equipe do OTO
-- Na planilha de tabulação a diária de cada tipo (e a geral) é digitada a partir
-- da Pesquisa de Diária Média. Quando a equipe informa a diária da campanha, ela
-- substitui no cálculo a média das diárias informadas nas respostas.
-- =============================================================================

CREATE TABLE campanha_diaria (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    campanha_id            BIGINT        NOT NULL REFERENCES campanha (id) ON DELETE CASCADE,
    tipo_hospedagem_codigo VARCHAR(20)   REFERENCES tipo_hospedagem (codigo),
    diaria_media           NUMERIC(10,2) NOT NULL,
    fonte                  VARCHAR(200),
    registrado_por         BIGINT        REFERENCES usuario (id),
    registrado_em          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_campanha_diaria UNIQUE NULLS NOT DISTINCT (campanha_id, tipo_hospedagem_codigo),
    CONSTRAINT ck_campanha_diaria_valor CHECK (diaria_media > 0)
);

COMMENT ON TABLE campanha_diaria IS 'Diária média digitada pela equipe para a campanha. Substitui a média das respostas no cálculo.';
COMMENT ON COLUMN campanha_diaria.tipo_hospedagem_codigo IS 'Nulo = diária média geral (todos os tipos).';
COMMENT ON COLUMN campanha_diaria.campanha_id IS 'Excluir a campanha (só possível em rascunho) exclui as diárias digitadas para ela.';
