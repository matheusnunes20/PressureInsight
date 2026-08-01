package com.pressuretestanalyzer.dto;

/** Corpo padrao das respostas de erro da API, montado pelo {@code GlobalExceptionHandler}. */
public record ErrorResponse(int status, String error, String message) {
}
