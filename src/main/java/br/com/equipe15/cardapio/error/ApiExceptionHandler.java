package br.com.equipe15.cardapio.error;

import br.com.equipe15.cardapio.produto.ProdutoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ProblemDetail produtoNaoEncontrado(ProdutoNaoEncontradoException exception) {
        return problem(HttpStatus.NOT_FOUND, "Produto não encontrado", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tipoInvalido() {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "O ID do produto deve ser um número inteiro válido.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
