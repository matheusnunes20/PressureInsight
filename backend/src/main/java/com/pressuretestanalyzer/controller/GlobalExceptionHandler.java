package com.pressuretestanalyzer.controller;

import com.pressuretestanalyzer.dto.ErrorResponse;
import com.pressuretestanalyzer.exception.IntervalNotFoundException;
import com.pressuretestanalyzer.exception.InvalidFileFormatException;
import com.pressuretestanalyzer.exception.UnsupportedSensorFileException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Converte as excecoes de dominio ja existentes e as de validacao do Spring
 * em respostas HTTP consistentes ({@link ErrorResponse}). Nenhuma regra de
 * negocio e reimplementada aqui - apenas o mapeamento excecao -> status HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileFormat(InvalidFileFormatException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(UnsupportedSensorFileException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedSensorFile(UnsupportedSensorFileException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(IntervalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIntervalNotFound(IntervalNotFoundException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
        return badRequest("Parametros da requisicao invalidos: " + e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Payload Too Large",
                        "Arquivo excede o tamanho maximo permitido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                        "Erro inesperado ao processar a solicitacao"));
    }

    private ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", message));
    }
}
