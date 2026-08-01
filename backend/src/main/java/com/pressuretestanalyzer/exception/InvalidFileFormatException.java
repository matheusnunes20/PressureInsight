package com.pressuretestanalyzer.exception;

/**
 * Thrown when a file is recognized by a parser but a specific line does not
 * conform to the expected structure (bad date, bad number, wrong column
 * count, ...).
 */
public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException(String fileName, int lineNumber, String line, String reason) {
        super("Arquivo %s invalido na linha %d (\"%s\"): %s".formatted(fileName, lineNumber, line, reason));
    }
}
