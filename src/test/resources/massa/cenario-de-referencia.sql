-- =============================================================================
-- Massa dos testes de aceitação (Plano de Testes, seção 3). Dados fictícios.
-- Roda depois das migrações, com o banco limpo, antes de cada cenário.
-- Senha de todos os usuários: Senha@Teste1
-- =============================================================================

INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) VALUES
    ('Equipe OTO',                'admin@oto.test',        '$2a$10$4evGw3GD0iRQRWVMBcP4TOtRmewRBduYGEGDPOzgVAwh1cU5zDwaC', 'ADMIN',        TRUE),
    ('Parceiro dos Hotéis A e B', 'parceiro.a@oto.test',   '$2a$10$4evGw3GD0iRQRWVMBcP4TOtRmewRBduYGEGDPOzgVAwh1cU5zDwaC', 'PARCEIRO',     TRUE),
    ('Parceiro da Pousada C',     'parceiro.c@oto.test',   '$2a$10$4evGw3GD0iRQRWVMBcP4TOtRmewRBduYGEGDPOzgVAwh1cU5zDwaC', 'PARCEIRO',     TRUE),
    ('Gestora da Secretaria',     'visualizador@oto.test', '$2a$10$4evGw3GD0iRQRWVMBcP4TOtRmewRBduYGEGDPOzgVAwh1cU5zDwaC', 'VISUALIZADOR', TRUE),
    ('Usuário inativo',           'inativo@oto.test',      '$2a$10$4evGw3GD0iRQRWVMBcP4TOtRmewRBduYGEGDPOzgVAwh1cU5zDwaC', 'VISUALIZADOR', FALSE);

INSERT INTO estabelecimento (tipo_hospedagem_codigo, situacao, nome_fantasia, cnpj, telefone_comercial, contato_nome, ativo) VALUES
    ('HOTEL',   'REGULAR',   'Hotel A',   '11111111000101', '17 3281-1001', 'Contato do Hotel A',   TRUE),
    ('HOTEL',   'REGULAR',   'Hotel B',   '22222222000102', '17 3281-1002', 'Contato do Hotel B',   TRUE),
    ('POUSADA', 'REGULAR',   'Pousada C', '33333333000103', '17 3281-1003', 'Contato da Pousada C', TRUE),
    ('FLAT',    'REGULAR',   'Flat D',    '44444444000104', '17 3281-1004', 'Contato do Flat D',    FALSE),
    ('HOSTEL',  'IRREGULAR', 'Hostel E',  '55555555000105', '17 3281-1005', 'Contato do Hostel E',  TRUE);

INSERT INTO capacidade_hospedagem (estabelecimento_id, vigente_desde, qtd_uh, qtd_leitos, max_leitos_por_uh)
SELECT e.id, DATE '2026-01-01', c.qtd_uh, c.qtd_leitos, c.max_leitos_por_uh
FROM (VALUES
        ('Hotel A',   10, 30, 4),
        ('Hotel B',   20, 50, 3),
        ('Pousada C', 10, 25, 4),
        ('Flat D',     8, 16, 2),
        ('Hostel E',   6, 24, 6)
     ) AS c (nome, qtd_uh, qtd_leitos, max_leitos_por_uh)
JOIN estabelecimento e ON e.nome_fantasia = c.nome;

INSERT INTO rhc_resumo (vigente_desde, qtd_registros, qtd_uh, qtd_leitos, max_leitos_por_uh)
VALUES (DATE '2026-01-01', 3, 4, 12, 3);

INSERT INTO usuario_estabelecimento (usuario_id, estabelecimento_id)
SELECT u.id, e.id
FROM (VALUES
        ('parceiro.a@oto.test', 'Hotel A'),
        ('parceiro.a@oto.test', 'Hotel B'),
        ('parceiro.c@oto.test', 'Pousada C')
     ) AS v (email, nome)
JOIN usuario u ON u.email = v.email
JOIN estabelecimento e ON e.nome_fantasia = v.nome;

-- Pesquisa 001/26, aberta, com a campanha consolidada do feriado de 1º de Maio.
INSERT INTO rodada_pesquisa (codigo, titulo, status, data_abertura, prazo_resposta, criado_por)
SELECT '001/26', 'Pesquisa do feriado de 1º de Maio 2026', 'ABERTA', DATE '2026-05-04', DATE '2026-05-15', id
FROM usuario WHERE email = 'admin@oto.test';

INSERT INTO campanha (rodada_id, nome, modalidade, tipo_periodo, feriado_codigo, periodo_inicio, periodo_fim, qtd_diarias)
SELECT id, '1º de Maio 2026', 'CONSOLIDADA', 'FERIADO', 'DIA_DO_TRABALHO', DATE '2026-05-01', DATE '2026-05-03', 2
FROM rodada_pesquisa WHERE codigo = '001/26';
