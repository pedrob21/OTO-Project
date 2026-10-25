package OTO.pesquisa;

public enum StatusRodada {
    /** Em montagem: campanhas podem ser incluídas e alteradas. */
    RASCUNHO,
    /** Recebendo respostas; o parceiro pode corrigir até o encerramento. */
    ABERTA,
    /** Fechada para respostas; as campanhas podem ser publicadas. */
    ENCERRADA
}
