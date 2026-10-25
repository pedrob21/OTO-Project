package OTO.comum.excecao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * converte as exceções em respostas no formato Problem Details (padrão RFC 9457):
 * {@code {"status": 422, "title": "...", "detail": "..."}}. Erros de validação
 * trazem também {@code "campos": {"campo": "mensagem"}}.
 *
 * <p>tem precedência sobre o tratador padrão do Spring Boot
 * ({@code spring.mvc.problemdetails.enabled}), que continua cuidando dos demais erros
 * do Spring MVC (rota inexistente, método não permitido etc.) no mesmo formato.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Operação não permitida", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Corrija os campos indicados.");
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        problema.setProperty("campos", campos);
        return problema;
    }

    /** restrições em parâmetros de rota ou de consulta (ex.: {@code @RequestParam @Min}). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail validacaoDeParametros(HandlerMethodValidationException e) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Corrija os parâmetros indicados.");
        Map<String, String> campos = new LinkedHashMap<>();
        for (ParameterValidationResult resultado : e.getParameterValidationResults()) {
            String nome = resultado.getMethodParameter().getParameterName();
            if (!resultado.getResolvableErrors().isEmpty()) {
                campos.putIfAbsent(nome == null ? "parametro" : nome,
                        resultado.getResolvableErrors().get(0).getDefaultMessage());
            }
        }
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpoInvalido(HttpMessageNotReadableException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O corpo não é um JSON válido ou tem algum valor em formato errado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Valor inválido para o parâmetro '" + e.getName() + "'.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflito(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Conflito",
                "A operação repete um registro que já existe ou deixa dados inconsistentes.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem acesso a este recurso.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
