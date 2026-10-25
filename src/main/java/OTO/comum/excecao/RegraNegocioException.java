package OTO.comum.excecao;

/** operação válida no formato, mas não permitida pelas regras do OTO. Vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
