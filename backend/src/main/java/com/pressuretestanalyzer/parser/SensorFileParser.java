package com.pressuretestanalyzer.parser;

import java.util.List;

/**
 * Strategy interface: one implementation per sensor manufacturer
 * (Teksensor, Additel, ...).
 */
public interface SensorFileParser {

    /**
     * Cheap structural check used by {@link SensorFileParserResolver} to pick
     * the right strategy for a file.
     */
    boolean supports(RawSensorFile file);

    /**
     * Converts every data line into a {@link PressureRecord}. Implementations
     * must throw {@code InvalidFileFormatException} on the first malformed
     * line rather than skipping it.
     */
    List<PressureRecord> parse(RawSensorFile file);
}
