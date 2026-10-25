package OTO.comum.excecao;

/** e-mail ou senha incorretos, ou usuário inativo. Vira HTTP 401. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos.");
    }
}
