-- =============================================================================
-- V3 - Estabelecimentos (inventário oficial), capacidade e hospedagem caseira
-- Colunas a partir da aba "Meios de Hospedagem" do Inventário Oficial.
-- Serviços, equipamentos, acessibilidade e indicadores ODS ficam no
-- catálogo do inventário (V4), e não em colunas.
-- =============================================================================

CREATE TABLE estabelecimento (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    segmento                 VARCHAR(30)  NOT NULL DEFAULT 'MEIO_HOSPEDAGEM',
    tipo_hospedagem_codigo   VARCHAR(20)  REFERENCES tipo_hospedagem (codigo),
    situacao                 VARCHAR(20)  NOT NULL DEFAULT 'REGULAR',
    nome_fantasia            VARCHAR(200) NOT NULL,
    razao_social             VARCHAR(200),
    cnpj                     VARCHAR(14),
    data_abertura            DATE,
    proprietarios            VARCHAR(300),
    cnae                     VARCHAR(20),

    -- Endereço e localização
    endereco                 VARCHAR(300),
    bairro                   VARCHAR(120),
    regiao                   VARCHAR(120),
    localizacao              VARCHAR(300),
    latitude                 NUMERIC(9,6),
    longitude                NUMERIC(9,6),

    -- Contato comercial
    telefone_comercial       VARCHAR(30),
    email_comercial          VARCHAR(254),
    site                     VARCHAR(300),
    facebook                 VARCHAR(300),
    instagram                VARCHAR(300),

    -- CADASTUR
    cadastur_exigencia       VARCHAR(20),
    possui_cadastur          BOOLEAN      NOT NULL DEFAULT FALSE,
    cadastur_numero          VARCHAR(30),
    cadastur_validade        DATE,

    -- Funcionamento e reservas
    funcionarios_fixos       INTEGER,
    funcionarios_temporarios INTEGER,
    funcionamento_24h        BOOLEAN,
    horario_check_in         TIME,
    horario_check_out        TIME,
    formas_reserva           VARCHAR(300),
    formas_pagamento         VARCHAR(300),

    -- Uso exclusivo da Secretaria (o parceiro não edita)
    contato_nome             VARCHAR(150),
    aceita_pesquisas         BOOLEAN      NOT NULL DEFAULT TRUE,
    telefone_pesquisas       VARCHAR(30),
    email_pesquisas          VARCHAR(254),
    destino_inteligente      BOOLEAN      NOT NULL DEFAULT FALSE,
    dados_pddt               BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Controle
    ativo                    BOOLEAN      NOT NULL DEFAULT TRUE,
    inventario_atualizado_em TIMESTAMPTZ,
    criado_em                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em            TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_estabelecimento_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_estabelecimento_segmento CHECK (segmento IN ('MEIO_HOSPEDAGEM')),
    CONSTRAINT ck_estabelecimento_tipo CHECK (segmento <> 'MEIO_HOSPEDAGEM' OR tipo_hospedagem_codigo IS NOT NULL),
    CONSTRAINT ck_estabelecimento_situacao CHECK (situacao IN ('REGULAR', 'IRREGULAR')),
    CONSTRAINT ck_estabelecimento_cnpj CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_estabelecimento_cadastur CHECK (cadastur_exigencia IN ('OBRIGATORIO', 'OPCIONAL', 'NAO_CONSTA')),
    CONSTRAINT ck_estabelecimento_funcionarios CHECK (funcionarios_fixos >= 0 AND funcionarios_temporarios >= 0),
    CONSTRAINT ck_estabelecimento_coordenadas CHECK (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
);

CREATE INDEX ix_estabelecimento_tipo ON estabelecimento (tipo_hospedagem_codigo);

COMMENT ON TABLE estabelecimento IS 'Empresas do trade turístico (hoje só meios de hospedagem). Não há exclusão: usa-se ativo = false.';
COMMENT ON COLUMN estabelecimento.segmento IS 'Novos segmentos (alimentação, atrativos etc.) entram em migrations futuras.';
COMMENT ON COLUMN estabelecimento.situacao IS 'IRREGULAR = meio de hospedagem sem regularização. Fica fora dos cálculos, como na série histórica.';
COMMENT ON COLUMN estabelecimento.cnpj IS 'Só dígitos.';
COMMENT ON COLUMN estabelecimento.cadastur_exigencia IS 'Coluna "CADASTUR 1=obrigatório 2=opcional 3=não consta" da planilha.';
COMMENT ON COLUMN estabelecimento.destino_inteligente IS 'Coluna "Filtro D = Destinos Inteligentes" da planilha.';
COMMENT ON COLUMN estabelecimento.dados_pddt IS 'Coluna "Dados PDDT" da planilha (significado a confirmar com o OTO).';
COMMENT ON COLUMN estabelecimento.inventario_atualizado_em IS 'Última vez que o inventário foi revisado pelo parceiro ou pela Secretaria.';

-- Capacidade (UH e leitos) com histórico: cada mudança cria uma nova linha,
-- para que os cálculos de um período usem a capacidade vigente naquele período.
CREATE TABLE capacidade_hospedagem (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estabelecimento_id BIGINT        NOT NULL REFERENCES estabelecimento (id),
    vigente_desde      DATE          NOT NULL,
    qtd_uh             INTEGER       NOT NULL,
    qtd_uh_pcd         INTEGER       NOT NULL DEFAULT 0,
    qtd_leitos         INTEGER       NOT NULL,
    max_leitos_por_uh  SMALLINT      NOT NULL,
    leitos_uteis       NUMERIC(10,1) GENERATED ALWAYS AS (qtd_uh * (1 + max_leitos_por_uh) / 2.0) STORED,
    registrado_por     BIGINT        REFERENCES usuario (id),
    registrado_em      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_capacidade_vigencia UNIQUE (estabelecimento_id, vigente_desde),
    CONSTRAINT ck_capacidade_valores CHECK (qtd_uh > 0 AND qtd_leitos > 0 AND max_leitos_por_uh > 0),
    CONSTRAINT ck_capacidade_uh_pcd CHECK (qtd_uh_pcd BETWEEN 0 AND qtd_uh)
);

COMMENT ON TABLE capacidade_hospedagem IS 'Capacidade de cada meio de hospedagem ao longo do tempo. A vigente em uma data é a de maior vigente_desde até aquela data.';
COMMENT ON COLUMN capacidade_hospedagem.qtd_leitos IS 'Total de leitos declarado. Usado no percentual de leitos respondentes.';
COMMENT ON COLUMN capacidade_hospedagem.max_leitos_por_uh IS 'Capacidade máxima de pessoas por UH (coluna "Leitos" da aba MH da tabulação).';
COMMENT ON COLUMN capacidade_hospedagem.leitos_uteis IS 'Metodologia do OTO: UH x média entre 1 e a capacidade máxima por UH.';

-- Hospedagem caseira (RHC): só os totais, que é o que entra no cálculo.
-- Os dados individuais (proprietário, CPF) não são guardados (LGPD).
CREATE TABLE rhc_resumo (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vigente_desde     DATE          NOT NULL,
    qtd_registros     INTEGER       NOT NULL,
    qtd_uh            INTEGER       NOT NULL,
    qtd_leitos        INTEGER       NOT NULL,
    max_leitos_por_uh SMALLINT      NOT NULL,
    leitos_uteis      NUMERIC(10,1) GENERATED ALWAYS AS (qtd_uh * (1 + max_leitos_por_uh) / 2.0) STORED,
    registrado_por    BIGINT        REFERENCES usuario (id),
    registrado_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_rhc_resumo_vigencia UNIQUE (vigente_desde),
    CONSTRAINT ck_rhc_resumo_valores CHECK (qtd_registros >= 0 AND qtd_uh >= 0 AND qtd_leitos >= 0 AND max_leitos_por_uh > 0)
);

COMMENT ON TABLE rhc_resumo IS 'Totais de Registros de Hospedagem Caseira (RHC) ao longo do tempo.';

-- Parceiros e os estabelecimentos que representam (um grupo pode ter vários
-- hotéis, e um hotel pode ter mais de um responsável).
CREATE TABLE usuario_estabelecimento (
    usuario_id         BIGINT      NOT NULL REFERENCES usuario (id),
    estabelecimento_id BIGINT      NOT NULL REFERENCES estabelecimento (id),
    criado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, estabelecimento_id)
);

CREATE INDEX ix_usuario_estabelecimento_estab ON usuario_estabelecimento (estabelecimento_id);

COMMENT ON TABLE usuario_estabelecimento IS 'Estabelecimentos que cada usuário PARCEIRO pode ver e editar.';
