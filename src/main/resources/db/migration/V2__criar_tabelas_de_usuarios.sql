-- =============================================================================
-- V2 - Usuários, tokens de acesso e auditoria
-- Perfis validados com o OTO na entrevista 1: administrador, parceiro
-- (empresa) e visualizador (gestor da Secretaria, só leitura).
-- =============================================================================

CREATE TABLE usuario (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome             VARCHAR(150) NOT NULL,
    email            VARCHAR(254) NOT NULL,
    senha_hash       VARCHAR(100),
    perfil           VARCHAR(20)  NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    ultimo_acesso_em TIMESTAMPTZ,
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = lower(email)),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'PARCEIRO', 'VISUALIZADOR'))
);

COMMENT ON TABLE usuario IS 'Usuários do sistema. Não há exclusão: um usuário desligado é inativado.';
COMMENT ON COLUMN usuario.email IS 'Sempre em minúsculas (a aplicação normaliza antes de gravar).';
COMMENT ON COLUMN usuario.senha_hash IS 'Hash BCrypt. Nulo enquanto o convidado não define a senha no primeiro acesso.';
COMMENT ON COLUMN usuario.perfil IS 'ADMIN: tudo, inclusive publicar resultados. PARCEIRO: só os próprios estabelecimentos. VISUALIZADOR: somente leitura.';

-- Tokens de uso único enviados por e-mail (convite e redefinição de senha).
CREATE TABLE token_usuario (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuario (id),
    tipo       VARCHAR(30)  NOT NULL,
    token_hash VARCHAR(100) NOT NULL,
    expira_em  TIMESTAMPTZ  NOT NULL,
    usado_em   TIMESTAMPTZ,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_token_usuario_hash UNIQUE (token_hash),
    CONSTRAINT ck_token_usuario_tipo CHECK (tipo IN ('CONVITE', 'REDEFINICAO_SENHA'))
);

CREATE INDEX ix_token_usuario_usuario ON token_usuario (usuario_id);

COMMENT ON TABLE token_usuario IS 'Tokens de convite e de redefinição de senha. Guarda só o hash (SHA-256); o token em si vai apenas no e-mail.';

-- Registro de alterações feitas pelos usuários (quem mudou o quê e quando).
CREATE TABLE auditoria (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    BIGINT      REFERENCES usuario (id),
    entidade      VARCHAR(60) NOT NULL,
    entidade_id   VARCHAR(60) NOT NULL,
    acao          VARCHAR(30) NOT NULL,
    dados_antes   JSONB,
    dados_depois  JSONB,
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_auditoria_entidade ON auditoria (entidade, entidade_id);
CREATE INDEX ix_auditoria_usuario ON auditoria (usuario_id);

COMMENT ON TABLE auditoria IS 'Histórico de alterações (ex.: parceiro atualizou o inventário, admin publicou uma campanha). Somente inserção.';
COMMENT ON COLUMN auditoria.acao IS 'Ex.: CRIAR, ATUALIZAR, INATIVAR, REATIVAR, ABRIR, ENCERRAR, PUBLICAR.';
