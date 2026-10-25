-- =============================================================================
-- V6 - Resultados publicados e série histórica
-- O resultado é calculado pela aplicação e gravado na publicação. A partir daí
-- fica congelado: mudanças posteriores no inventário não alteram boletins antigos.
-- =============================================================================

CREATE TABLE resultado_campanha (
    campanha_id                    BIGINT        PRIMARY KEY REFERENCES campanha (id),
    parametro_calculo_id           BIGINT        NOT NULL REFERENCES parametro_calculo (id),
    taxa_ocupacao                  NUMERIC(5,2)  NOT NULL,
    qtd_mh                         INTEGER       NOT NULL,
    qtd_mh_respondentes            INTEGER       NOT NULL,
    leitos_total                   INTEGER       NOT NULL,
    leitos_respondentes            INTEGER       NOT NULL,
    percentual_leitos_respondentes NUMERIC(5,2)  NOT NULL,
    fluxo_mh                       NUMERIC(14,2) NOT NULL,
    fluxo_rhc                      NUMERIC(14,2) NOT NULL,
    fluxo_day_use                  NUMERIC(14,2) NOT NULL,
    fluxo_total                    NUMERIC(14,2) NOT NULL,
    fluxo_medio_diario             NUMERIC(14,2) NOT NULL,
    receita_hospedagem             NUMERIC(16,2),
    receita_sem_hospedagem         NUMERIC(16,2),
    calculado_em                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_resultado_taxa CHECK (taxa_ocupacao BETWEEN 0 AND 100),
    CONSTRAINT ck_resultado_mh CHECK (qtd_mh_respondentes BETWEEN 0 AND qtd_mh),
    CONSTRAINT ck_resultado_leitos CHECK (leitos_respondentes BETWEEN 0 AND leitos_total
        AND percentual_leitos_respondentes BETWEEN 0 AND 100),
    CONSTRAINT ck_resultado_fluxo CHECK (fluxo_mh >= 0 AND fluxo_rhc >= 0 AND fluxo_day_use >= 0 AND fluxo_total >= 0)
);

COMMENT ON TABLE resultado_campanha IS 'Resultado consolidado de uma campanha publicada (dados globais, sem identificar estabelecimentos).';
COMMENT ON COLUMN resultado_campanha.leitos_total IS 'Leitos declarados dos meios de hospedagem regulares + RHC.';
COMMENT ON COLUMN resultado_campanha.fluxo_total IS 'Fluxo de turistas em pernoites, como na metodologia do OTO (não são pessoas distintas).';
COMMENT ON COLUMN resultado_campanha.receita_hospedagem IS 'RevPAR x UH x diárias, somado por tipo. Nulo quando a campanha não tem diária média.';
COMMENT ON COLUMN resultado_campanha.receita_sem_hospedagem IS 'Fluxo total x (ingresso x dias em parques + gasto diário x dias de permanência).';

CREATE TABLE resultado_campanha_tipo (
    campanha_id            BIGINT        NOT NULL REFERENCES resultado_campanha (campanha_id),
    tipo_hospedagem_codigo VARCHAR(20)   NOT NULL REFERENCES tipo_hospedagem (codigo),
    qtd_mh                 INTEGER       NOT NULL,
    qtd_mh_respondentes    INTEGER       NOT NULL,
    qtd_uh                 INTEGER       NOT NULL,
    taxa_ocupacao          NUMERIC(5,2),
    diaria_media           NUMERIC(10,2),
    revpar                 NUMERIC(10,2),
    receita_estimada       NUMERIC(16,2),
    PRIMARY KEY (campanha_id, tipo_hospedagem_codigo),
    CONSTRAINT ck_resultado_tipo_mh CHECK (qtd_mh_respondentes BETWEEN 0 AND qtd_mh AND qtd_uh >= 0),
    CONSTRAINT ck_resultado_tipo_taxa CHECK (taxa_ocupacao BETWEEN 0 AND 100)
);

COMMENT ON TABLE resultado_campanha_tipo IS 'Resultado por tipo de hospedagem. Na exibição, recortes com poucos estabelecimentos devem ser omitidos para não expor ninguém.';

-- Série histórica importada da planilha "Indicadores do Turismo" (2017 em diante).
CREATE TABLE indicador_historico (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    indicador              VARCHAR(30)   NOT NULL,
    modalidade             VARCHAR(20)   NOT NULL,
    ano                    SMALLINT      NOT NULL,
    mes                    SMALLINT,
    feriado_codigo         VARCHAR(40)   REFERENCES feriado_evento (codigo),
    tipo_hospedagem_codigo VARCHAR(20)   REFERENCES tipo_hospedagem (codigo),
    valor                  NUMERIC(14,2) NOT NULL,
    observacao             VARCHAR(300),
    fonte                  VARCHAR(200)  NOT NULL,
    importado_em           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_indicador_historico UNIQUE NULLS NOT DISTINCT
        (indicador, modalidade, ano, mes, feriado_codigo, tipo_hospedagem_codigo),
    CONSTRAINT ck_indicador_historico_indicador CHECK (indicador IN ('TAXA_OCUPACAO', 'FLUXO_TURISTAS', 'DIARIA_MEDIA')),
    CONSTRAINT ck_indicador_historico_modalidade CHECK (modalidade IN ('CONSOLIDADA', 'EXPECTATIVA')),
    CONSTRAINT ck_indicador_historico_ano CHECK (ano BETWEEN 2000 AND 2100),
    CONSTRAINT ck_indicador_historico_periodo CHECK ((mes IS NULL) <> (feriado_codigo IS NULL)),
    CONSTRAINT ck_indicador_historico_mes CHECK (mes BETWEEN 1 AND 12)
);

COMMENT ON TABLE indicador_historico IS 'Valores anteriores ao sistema, para os comparativos ano a ano. Cada valor é de um mês ou de um feriado.';
COMMENT ON COLUMN indicador_historico.tipo_hospedagem_codigo IS 'Nulo = valor geral (todos os tipos).';
COMMENT ON COLUMN indicador_historico.observacao IS 'Notas da planilha, ex.: período unificado de Natal e Réveillon.';
