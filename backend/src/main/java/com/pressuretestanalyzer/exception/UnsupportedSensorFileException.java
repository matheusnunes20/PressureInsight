package com.pressuretestanalyzer.exception;

/** Thrown when no registered {@code SensorFileParser} recognizes a file. */
public class UnsupportedSensorFileException extends RuntimeException {

    public UnsupportedSensorFileException(String fileName) {
        super("Nenhum parser suporta o arquivo: " + fileName);
    }
}
