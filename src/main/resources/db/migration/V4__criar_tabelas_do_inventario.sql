-- =============================================================================
-- V4 - Catálogo do inventário (serviços, equipamentos, acessibilidade e ODS)
-- A planilha do inventário tem ~150 colunas de marcar (sim/não). Em vez de uma
-- coluna para cada, o catálogo lista grupos e itens, e cada estabelecimento
-- guarda só os itens que possui. O OTO pode incluir itens sem mudar o banco.
-- =============================================================================

CREATE TABLE inventario_grupo (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    segmento             VARCHAR(30)  NOT NULL DEFAULT 'MEIO_HOSPEDAGEM',
    categoria            VARCHAR(30)  NOT NULL,
    codigo               VARCHAR(60)  NOT NULL,
    nome                 VARCHAR(150) NOT NULL,
    exclusivo_secretaria BOOLEAN      NOT NULL DEFAULT FALSE,
    ordem                SMALLINT     NOT NULL,
    ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_inventario_grupo_codigo UNIQUE (segmento, codigo),
    CONSTRAINT ck_inventario_grupo_segmento CHECK (segmento IN ('MEIO_HOSPEDAGEM')),
    CONSTRAINT ck_inventario_grupo_categoria CHECK (categoria IN ('SERVICOS_EQUIPAMENTOS', 'ACESSIBILIDADE', 'SUSTENTABILIDADE'))
);

COMMENT ON TABLE inventario_grupo IS 'Grupos de perguntas do inventário (ex.: Área de recreação, Rampa, Indicadores ODS).';
COMMENT ON COLUMN inventario_grupo.exclusivo_secretaria IS 'Grupos preenchidos só pela Secretaria (ex.: indicadores ODS). O parceiro vê, mas não edita.';

CREATE TABLE inventario_item (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    grupo_id            BIGINT       NOT NULL REFERENCES inventario_grupo (id),
    codigo              VARCHAR(80)  NOT NULL,
    nome                VARCHAR(200) NOT NULL,
    descricao           VARCHAR(300),
    permite_complemento BOOLEAN      NOT NULL DEFAULT FALSE,
    ordem               SMALLINT     NOT NULL,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_inventario_item_codigo UNIQUE (grupo_id, codigo)
);

COMMENT ON TABLE inventario_item IS 'Opções de cada grupo (ex.: Piscina, Sauna). A ausência de itens marcados equivale ao "Não" da planilha.';
COMMENT ON COLUMN inventario_item.permite_complemento IS 'Itens como "Outros", que pedem um texto livre.';

CREATE TABLE estabelecimento_inventario_item (
    estabelecimento_id BIGINT       NOT NULL REFERENCES estabelecimento (id),
    item_id            BIGINT       NOT NULL REFERENCES inventario_item (id),
    complemento        VARCHAR(300),
    registrado_por     BIGINT       REFERENCES usuario (id),
    registrado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (estabelecimento_id, item_id)
);

CREATE INDEX ix_estab_inventario_item_item ON estabelecimento_inventario_item (item_id);

COMMENT ON TABLE estabelecimento_inventario_item IS 'Itens do inventário que cada estabelecimento possui. Mudanças ficam registradas na tabela auditoria.';
