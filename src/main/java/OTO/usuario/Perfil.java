package OTO.usuario;

/** Perfis validados com o OTO na entrevista 1. */
public enum Perfil {
    /** Equipe do OTO: tudo, inclusive publicar resultados. */
    ADMIN,
    /** Empresa parceira: só os próprios estabelecimentos. */
    PARCEIRO,
    /** Gestor da Secretaria: somente leitura. */
    VISUALIZADOR
}
