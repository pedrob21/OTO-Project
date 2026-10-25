package OTO.comum.excecao;

/** recurso pedido não existe. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(recurso + " " + id + " não encontrado(a).");
    }
}
