package com.umc.pediupatrao.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(NoSuchElementException e, HttpServletRequest req) {
        String msg = e.getMessage() == null || "No value present".equals(e.getMessage())
                ? "Recurso não encontrado." : e.getMessage();
        return resposta(HttpStatus.NOT_FOUND, msg, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> requisicaoInvalida(IllegalArgumentException e, HttpServletRequest req) {
        String msg = e.getMessage() == null ? "Requisição inválida." : e.getMessage();
        return resposta(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> dadosInvalidos(BindException e, HttpServletRequest req) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return resposta(HttpStatus.BAD_REQUEST, msg.isBlank() ? "Requisição inválida." : msg, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpoIlegivel(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e,
            HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + e.getName() + "'.", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> parametroAusente(MissingServletRequestParameterException e,
            HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: '" + e.getParameterName() + "'.", req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> conflito(RegraNegocioException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(AccessDeniedException e, HttpServletRequest req) {
        log.warn("Acesso negado (método seguro): {} {}", req.getMethod(), req.getRequestURI());
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", req);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String erro, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErroApi.corpo(status.value(), erro, req.getRequestURI()));
    }
}
