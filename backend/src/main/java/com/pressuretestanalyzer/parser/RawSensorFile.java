package com.pressuretestanalyzer.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * In-memory representation of an uploaded sensor file, decoupled from any
 * web/servlet type so parsers can be unit tested without Spring.
 */
public record RawSensorFile(String fileName, List<String> lines) {

    public static RawSensorFile from(String fileName, InputStream content) {
        try (InputStream in = content) {
            List<String> lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            return new RawSensorFile(fileName, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
