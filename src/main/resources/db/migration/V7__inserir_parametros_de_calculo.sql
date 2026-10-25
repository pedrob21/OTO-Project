-- =============================================================================
-- V7 - Parâmetros iniciais do cálculo
-- Valores das planilhas de tabulação 2026, confirmados pelo OTO na entrevista 1:
--   - day use de 15,15% (Pesquisa de Demanda 2024);
--   - permanência média de 4 dias, sendo 2 em parques;
--   - R$ 250 de gasto diário e R$ 105 de ingresso (médias de inteira e meia);
--   - publicação a partir de 70% dos leitos respondentes.
-- Mudanças futuras entram como nova linha (nova vigência), pela aplicação.
-- =============================================================================

INSERT INTO parametro_calculo (
    vigente_desde,
    percentual_day_use,
    dias_permanencia_media,
    dias_em_parques,
    gasto_medio_diario,
    gasto_medio_ingresso,
    percentual_minimo_leitos_publicacao,
    fonte
) VALUES (
    DATE '2024-01-01',
    15.15,
    4,
    2,
    250.00,
    105.00,
    70.00,
    'Pesquisa de Demanda Turística 2024 e entrevista com o OTO (outubro/2026)'
);
