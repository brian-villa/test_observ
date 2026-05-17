package com.example.sgmta.exceptions;

import com.example.sgmta.dtos.exception.StandardErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Intercetor global de exceções.
 * Captura erros lançados pela aplicação e formata a resposta HTTP.
 */
@RestControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardErrorDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        StandardErrorDTO error = new StandardErrorDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<StandardErrorDTO> handleResourceConflict(ResourceConflictException ex, HttpServletRequest request) {
        StandardErrorDTO error = new StandardErrorDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<StandardErrorDTO> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        StandardErrorDTO error = new StandardErrorDTO(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<StandardErrorDTO> handleUnsupportedMediaType(UnsupportedMediaTypeException ex, HttpServletRequest request) {
        StandardErrorDTO error = new StandardErrorDTO(
                LocalDateTime.now(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<StandardErrorDTO> handleInvalidPayload(InvalidPayloadException ex, HttpServletRequest request) {
        StandardErrorDTO error = new StandardErrorDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Captura as regras de negócio quebradas ou excepções não mapeadas.
     * RuntimeException genérica
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardErrorDTO> handleBusinessRules(RuntimeException ex, HttpServletRequest request) {

        StandardErrorDTO error = new StandardErrorDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Captura os erros de validação dos DTOs.
     * Ocorre quando os dados do RequestBody falham nas anotações de validação.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardErrorDTO> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        StandardErrorDTO error = new StandardErrorDTO(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Erro de validação: " + errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
}
