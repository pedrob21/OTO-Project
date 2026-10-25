-- =============================================================================
-- V1 - Tabelas de referência
-- Domínios fixos usados pelas demais tabelas. Os valores vêm das planilhas do OTO.
-- =============================================================================

-- Tipos de meio de hospedagem do inventário oficial.
CREATE TABLE tipo_hospedagem (
    codigo          VARCHAR(20) PRIMARY KEY,
    nome            VARCHAR(60) NOT NULL,
    calcula_receita BOOLEAN     NOT NULL DEFAULT FALSE,
    ordem           SMALLINT    NOT NULL
);

COMMENT ON TABLE tipo_hospedagem IS 'Tipos de meio de hospedagem do inventário oficial.';
COMMENT ON COLUMN tipo_hospedagem.calcula_receita IS 'Se o tipo entra no cálculo de diária média, RevPAR e receita estimada (hoje: hotel, resort e pousada).';

INSERT INTO tipo_hospedagem (codigo, nome, calcula_receita, ordem) VALUES
    ('HOTEL',         'Hotel',         TRUE,  1),
    ('RESORT',        'Resort',        TRUE,  2),
    ('POUSADA',       'Pousada',       TRUE,  3),
    ('HOTEL_FAZENDA', 'Hotel fazenda', FALSE, 4),
    ('FLAT',          'Flat',          FALSE, 5),
    ('HOSTEL',        'Hostel',        FALSE, 6);

-- Feriados prolongados e eventos pesquisados pelo OTO
-- (lista da planilha "Indicadores do Turismo 2017-2025").
CREATE TABLE feriado_evento (
    codigo VARCHAR(40)  PRIMARY KEY,
    nome   VARCHAR(100) NOT NULL,
    ordem  SMALLINT     NOT NULL
);

COMMENT ON TABLE feriado_evento IS 'Feriados prolongados e eventos com pesquisa própria de ocupação. Permite comparar o mesmo feriado entre anos.';

INSERT INTO feriado_evento (codigo, nome, ordem) VALUES
    ('CARNAVAL',                'Carnaval',                                1),
    ('PASCOA',                  'Páscoa',                                  2),
    ('TIRADENTES',              'Tiradentes',                              3),
    ('DIA_DO_TRABALHO',         '1º de Maio',                              4),
    ('CORPUS_CHRISTI',          'Corpus Christi',                          5),
    ('FEFOL',                   'Festival do Folclore de Olímpia (FEFOL)', 6),
    ('FESTA_DO_PEAO',           'Festa do Peão de Boiadeiro de Barretos',  7),
    ('INDEPENDENCIA',           '7 de Setembro',                           8),
    ('NOSSA_SENHORA_APARECIDA', '12 de Outubro',                           9),
    ('SEMANA_DO_SACO_CHEIO',    'Semana do Saco Cheio',                   10),
    ('FINADOS',                 '2 de Novembro',                          11),
    ('PROCLAMACAO_REPUBLICA',   '15 de Novembro',                         12),
    ('CONSCIENCIA_NEGRA',       '20 de Novembro',                         13),
    ('NATAL',                   'Natal',                                  14),
    ('REVEILLON',               'Réveillon',                              15),
    ('NATAL_REVEILLON',         'Natal e Réveillon (período unificado)',  16);

-- Faixas de classificação da taxa de ocupação usadas nos boletins.
CREATE TABLE faixa_ocupacao (
    codigo            VARCHAR(20)  PRIMARY KEY,
    nome              VARCHAR(40)  NOT NULL,
    percentual_minimo NUMERIC(5,2) NOT NULL,
    CONSTRAINT uk_faixa_ocupacao_minimo UNIQUE (percentual_minimo),
    CONSTRAINT ck_faixa_ocupacao_minimo CHECK (percentual_minimo BETWEEN 0 AND 100)
);

COMMENT ON TABLE faixa_ocupacao IS 'Faixas dos boletins. A faixa de uma taxa é a de maior percentual_minimo que não passa da taxa.';

INSERT INTO faixa_ocupacao (codigo, nome, percentual_minimo) VALUES
    ('RUIM',      'Ruim',       0),
    ('REGULAR',   'Regular',   21),
    ('BOA',       'Boa',       41),
    ('OTIMA',     'Ótima',     61),
    ('EXCELENTE', 'Excelente', 81);
