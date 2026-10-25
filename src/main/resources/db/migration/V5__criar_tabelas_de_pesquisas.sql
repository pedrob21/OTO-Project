-- =============================================================================
-- V5 - Pesquisas de taxa de ocupação
-- Rodada = o envio feito aos parceiros (ex.: 005/26). Uma rodada costuma juntar
-- várias campanhas: "consolidada de fevereiro + Carnaval + expectativa de Páscoa".
-- Campanha = cada período medido, que gera um resultado e um boletim.
-- =============================================================================

-- Parâmetros da metodologia, com histórico. Valores iniciais em V7.
CREATE TABLE parametro_calculo (
    id                                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vigente_desde                       DATE          NOT NULL,
    percentual_day_use                  NUMERIC(5,2)  NOT NULL,
    dias_permanencia_media              SMALLINT      NOT NULL,
    dias_em_parques                     SMALLINT      NOT NULL,
    gasto_medio_diario                  NUMERIC(10,2) NOT NULL,
    gasto_medio_ingresso                NUMERIC(10,2) NOT NULL,
    percentual_minimo_leitos_publicacao NUMERIC(5,2)  NOT NULL,
    fonte                               VARCHAR(300)  NOT NULL,
    registrado_por                      BIGINT        REFERENCES usuario (id),
    registrado_em                       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_parametro_calculo_vigencia UNIQUE (vigente_desde),
    CONSTRAINT ck_parametro_calculo_percentuais CHECK (percentual_day_use >= 0 AND percentual_day_use < 100
        AND percentual_minimo_leitos_publicacao BETWEEN 0 AND 100),
    CONSTRAINT ck_parametro_calculo_dias CHECK (dias_permanencia_media > 0 AND dias_em_parques BETWEEN 0 AND dias_permanencia_media),
    CONSTRAINT ck_parametro_calculo_valores CHECK (gasto_medio_diario >= 0 AND gasto_medio_ingresso >= 0)
);

COMMENT ON TABLE parametro_calculo IS 'Parâmetros do cálculo de fluxo e receita. Vale a linha de maior vigente_desde até a data da campanha.';
COMMENT ON COLUMN parametro_calculo.percentual_day_use IS 'Parcela de turistas que não pernoitam (Pesquisa de Demanda).';
COMMENT ON COLUMN parametro_calculo.gasto_medio_diario IS 'Gasto médio por pessoa por dia, sem hospedagem (R$).';
COMMENT ON COLUMN parametro_calculo.gasto_medio_ingresso IS 'Gasto médio por pessoa com ingresso de parque, por dia (média de inteira e meia, R$).';
COMMENT ON COLUMN parametro_calculo.percentual_minimo_leitos_publicacao IS 'Mínimo de leitos respondentes para publicar o resultado de uma campanha.';

CREATE TABLE rodada_pesquisa (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo         VARCHAR(20)  NOT NULL,
    titulo         VARCHAR(200) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'RASCUNHO',
    data_abertura  DATE,
    prazo_resposta DATE,
    encerrada_em   TIMESTAMPTZ,
    criado_por     BIGINT       NOT NULL REFERENCES usuario (id),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_rodada_pesquisa_codigo UNIQUE (codigo),
    CONSTRAINT ck_rodada_pesquisa_status CHECK (status IN ('RASCUNHO', 'ABERTA', 'ENCERRADA')),
    CONSTRAINT ck_rodada_pesquisa_datas CHECK (status = 'RASCUNHO' OR (data_abertura IS NOT NULL AND prazo_resposta IS NOT NULL)),
    CONSTRAINT ck_rodada_pesquisa_prazo CHECK (prazo_resposta >= data_abertura),
    CONSTRAINT ck_rodada_pesquisa_encerramento CHECK ((status = 'ENCERRADA') = (encerrada_em IS NOT NULL))
);

COMMENT ON TABLE rodada_pesquisa IS 'Pesquisa enviada aos parceiros. Só recebe respostas com status ABERTA; o parceiro pode corrigir até o encerramento.';
COMMENT ON COLUMN rodada_pesquisa.codigo IS 'Numeração do OTO, ex.: 005/26.';

CREATE TABLE campanha (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rodada_id      BIGINT       NOT NULL REFERENCES rodada_pesquisa (id),
    nome           VARCHAR(150) NOT NULL,
    modalidade     VARCHAR(20)  NOT NULL,
    tipo_periodo   VARCHAR(20)  NOT NULL,
    feriado_codigo VARCHAR(40)  REFERENCES feriado_evento (codigo),
    periodo_inicio DATE         NOT NULL,
    periodo_fim    DATE         NOT NULL,
    qtd_diarias    SMALLINT     NOT NULL,
    publicada_em   TIMESTAMPTZ,
    publicada_por  BIGINT       REFERENCES usuario (id),
    CONSTRAINT uk_campanha_periodo UNIQUE NULLS NOT DISTINCT (modalidade, tipo_periodo, feriado_codigo, periodo_inicio),
    CONSTRAINT ck_campanha_modalidade CHECK (modalidade IN ('CONSOLIDADA', 'EXPECTATIVA')),
    CONSTRAINT ck_campanha_tipo_periodo CHECK (tipo_periodo IN ('MENSAL', 'FERIADO')),
    CONSTRAINT ck_campanha_feriado CHECK ((tipo_periodo = 'FERIADO') = (feriado_codigo IS NOT NULL)),
    CONSTRAINT ck_campanha_periodo CHECK (periodo_fim >= periodo_inicio),
    CONSTRAINT ck_campanha_diarias CHECK (qtd_diarias > 0),
    CONSTRAINT ck_campanha_publicacao CHECK ((publicada_em IS NULL) = (publicada_por IS NULL))
);

CREATE INDEX ix_campanha_rodada ON campanha (rodada_id);

COMMENT ON TABLE campanha IS 'Período medido dentro de uma rodada (mês ou feriado), na modalidade consolidada (realizado) ou expectativa (previsão).';
COMMENT ON COLUMN campanha.qtd_diarias IS 'Quantidade de diárias do período (ex.: 31 no mês, 5 no Carnaval). Multiplica a ocupação no cálculo do fluxo.';
COMMENT ON COLUMN campanha.publicada_em IS 'Preenchido quando o ADMIN publica. Só pode publicar com o mínimo de leitos respondentes do parâmetro vigente.';

-- Resposta atual de cada estabelecimento em cada campanha (vale o último envio).
CREATE TABLE resposta_ocupacao (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    campanha_id        BIGINT        NOT NULL REFERENCES campanha (id),
    estabelecimento_id BIGINT        NOT NULL REFERENCES estabelecimento (id),
    taxa_ocupacao      NUMERIC(5,2)  NOT NULL,
    diaria_media       NUMERIC(10,2),
    versao             INTEGER       NOT NULL,
    enviado_por        BIGINT        NOT NULL REFERENCES usuario (id),
    enviado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_resposta_campanha_estab UNIQUE (campanha_id, estabelecimento_id),
    CONSTRAINT ck_resposta_taxa CHECK (taxa_ocupacao BETWEEN 0 AND 100),
    CONSTRAINT ck_resposta_diaria CHECK (diaria_media > 0),
    CONSTRAINT ck_resposta_versao CHECK (versao > 0)
);

CREATE INDEX ix_resposta_estabelecimento ON resposta_ocupacao (estabelecimento_id);

COMMENT ON TABLE resposta_ocupacao IS 'Resposta vigente de cada meio de hospedagem por campanha. Dado individual e sigiloso: só sai do sistema agregado.';
COMMENT ON COLUMN resposta_ocupacao.taxa_ocupacao IS 'Percentual de 0 a 100.';
COMMENT ON COLUMN resposta_ocupacao.diaria_media IS 'Opcional. Diária de 1 apartamento standard para 2 adultos e 1 criança (R$).';
COMMENT ON COLUMN resposta_ocupacao.versao IS 'Número do envio. Cada correção soma 1 e grava uma linha em resposta_ocupacao_historico.';

-- Todos os envios, inclusive o atual. Somente inserção: correções não apagam nada.
CREATE TABLE resposta_ocupacao_historico (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resposta_id   BIGINT        NOT NULL REFERENCES resposta_ocupacao (id),
    versao        INTEGER       NOT NULL,
    taxa_ocupacao NUMERIC(5,2)  NOT NULL,
    diaria_media  NUMERIC(10,2),
    enviado_por   BIGINT        NOT NULL REFERENCES usuario (id),
    enviado_em    TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_resposta_historico_versao UNIQUE (resposta_id, versao),
    CONSTRAINT ck_resposta_historico_taxa CHECK (taxa_ocupacao BETWEEN 0 AND 100),
    CONSTRAINT ck_resposta_historico_diaria CHECK (diaria_media > 0)
);

COMMENT ON TABLE resposta_ocupacao_historico IS 'Histórico de envios de cada resposta, para rastrear correções.';
