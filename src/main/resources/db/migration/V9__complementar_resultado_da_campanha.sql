-- =============================================================================
-- V9 - Complementos do resultado publicado
-- Diária média geral e quantidade de respondentes por faixa de ocupação, como no
-- quadro "% FAIXA DE OCUPAÇÃO DOS MH RESPONDENTES" da tabulação. Os dois são
-- gravados na publicação, junto com o restante do resultado, e não mudam depois.
-- =============================================================================

ALTER TABLE resultado_campanha ADD COLUMN diaria_media NUMERIC(10,2);
ALTER TABLE resultado_campanha ADD CONSTRAINT ck_resultado_diaria CHECK (diaria_media > 0);

COMMENT ON COLUMN resultado_campanha.diaria_media IS 'Diária média geral, ponderada pelos leitos dos respondentes que informaram a diária. Nula quando ninguém informou.';

CREATE TABLE resultado_campanha_faixa (
    campanha_id  BIGINT      NOT NULL REFERENCES resultado_campanha (campanha_id),
    faixa_codigo VARCHAR(20) NOT NULL REFERENCES faixa_ocupacao (codigo),
    qtd_mh       INTEGER     NOT NULL,
    PRIMARY KEY (campanha_id, faixa_codigo),
    CONSTRAINT ck_resultado_faixa_qtd CHECK (qtd_mh >= 0)
);

COMMENT ON TABLE resultado_campanha_faixa IS 'Quantos meios de hospedagem respondentes ficaram em cada faixa de ocupação (Ruim a Excelente).';
