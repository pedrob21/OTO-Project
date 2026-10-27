package OTO.comum.excecao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class TratadorDeErrosTest {

    private final TratadorDeErros tratador = new TratadorDeErros();

    @Test
    void naoEncontradoVira404() {
        ProblemDetail p = tratador.naoEncontrado(RecursoNaoEncontradoException.de("Campanha", 9));
        assertThat(p.getStatus()).isEqualTo(404);
        assertThat(p.getTitle()).isEqualTo("Não encontrado");
        assertThat(p.getDetail()).isEqualTo("Campanha 9 não encontrado(a).");
    }

    @Test
    void credenciaisInvalidasVira401() {
        ProblemDetail p = tratador.credenciaisInvalidas(new CredenciaisInvalidasException());
        assertThat(p.getStatus()).isEqualTo(401);
        assertThat(p.getDetail()).isEqualTo("E-mail ou senha incorretos.");
    }

    @Test
    void regraDeNegocioVira422() {
        ProblemDetail p = tratador.regraDeNegocio(new RegraNegocioException("Pesquisa fechada."));
        assertThat(p.getStatus()).isEqualTo(422);
        assertThat(p.getTitle()).isEqualTo("Operação não permitida");
        assertThat(p.getDetail()).isEqualTo("Pesquisa fechada.");
    }

    @Test
    void validacaoDoCorpoListaOPrimeiroErroDeCadaCampo() throws Exception {
        BeanPropertyBindingResult erros = new BeanPropertyBindingResult(new Object(), "requisicao");
        erros.addError(new FieldError("requisicao", "email", "não deve estar em branco"));
        erros.addError(new FieldError("requisicao", "email", "deve ser um e-mail"));
        erros.addError(new FieldError("requisicao", "senha", "não deve estar em branco"));
        MethodParameter parametro = new MethodParameter(getClass().getDeclaredMethod("metodo", String.class), 0);

        ProblemDetail p = tratador.validacao(new MethodArgumentNotValidException(parametro, erros));

        assertThat(p.getStatus()).isEqualTo(400);
        assertThat(p.getProperties()).containsEntry("campos",
                Map.of("email", "não deve estar em branco", "senha", "não deve estar em branco"));
    }

    @Test
    void validacaoDeParametrosUsaONomeDoParametro() {
        HandlerMethodValidationException e = mock(HandlerMethodValidationException.class);
        List<ParameterValidationResult> resultados = List.of(
                resultado("ano", "deve ser maior que ou igual a 2000"),
                resultado(null, "inválido"),
                resultado("semErro", null));
        when(e.getParameterValidationResults()).thenReturn(resultados);

        ProblemDetail p = tratador.validacaoDeParametros(e);

        assertThat(p.getStatus()).isEqualTo(400);
        assertThat(p.getProperties()).containsEntry("campos",
                Map.of("ano", "deve ser maior que ou igual a 2000", "parametro", "inválido"));
    }

    @Test
    void corpoIlegivelVira400() {
        ProblemDetail p = tratador.corpoInvalido(new HttpMessageNotReadableException("x", mock(HttpInputMessage.class)));
        assertThat(p.getStatus()).isEqualTo(400);
        assertThat(p.getTitle()).isEqualTo("Requisição inválida");
    }

    @Test
    void parametroDeTipoErradoVira400ComONome() throws Exception {
        MethodParameter parametro = new MethodParameter(getClass().getDeclaredMethod("metodo", String.class), 0);
        ProblemDetail p = tratador.parametroInvalido(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", parametro, null));
        assertThat(p.getStatus()).isEqualTo(400);
        assertThat(p.getDetail()).isEqualTo("Valor inválido para o parâmetro 'id'.");
    }

    @Test
    void violacaoDeIntegridadeVira409() {
        assertThat(tratador.conflito(new DataIntegrityViolationException("dup")).getStatus()).isEqualTo(409);
    }

    @Test
    void acessoNegadoVira403() {
        ProblemDetail p = tratador.acessoNegado(new AccessDeniedException("não"));
        assertThat(p.getStatus()).isEqualTo(403);
        assertThat(p.getDetail()).isEqualTo("Seu perfil não tem acesso a este recurso.");
    }

    private static ParameterValidationResult resultado(String nome, String mensagem) {
        ParameterValidationResult resultado = mock(ParameterValidationResult.class);
        MethodParameter parametro = mock(MethodParameter.class);
        when(parametro.getParameterName()).thenReturn(nome);
        when(resultado.getMethodParameter()).thenReturn(parametro);
        if (mensagem == null) {
            when(resultado.getResolvableErrors()).thenReturn(List.of());
        }
        else {
            MessageSourceResolvable erro = mock(MessageSourceResolvable.class);
            when(erro.getDefaultMessage()).thenReturn(mensagem);
            when(resultado.getResolvableErrors()).thenReturn(List.of(erro));
        }
        return resultado;
    }

    @SuppressWarnings("unused")
    private void metodo(String valor) {
    }
}
