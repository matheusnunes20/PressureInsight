package com.pressuretestanalyzer.parser;

import com.pressuretestanalyzer.exception.UnsupportedSensorFileException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Picks the {@link SensorFileParser} strategy that recognizes a given file.
 * New sensor formats are supported by adding another {@code SensorFileParser}
 * bean, without changing this class.
 */
@Component
public class SensorFileParserResolver {

    private final List<SensorFileParser> parsers;

    public SensorFileParserResolver(List<SensorFileParser> parsers) {
        this.parsers = parsers;
    }

    public SensorFileParser resolve(RawSensorFile file) {
        return parsers.stream()
                .filter(parser -> parser.supports(file))
                .findFirst()
                .orElseThrow(() -> new UnsupportedSensorFileException(file.fileName()));
    }
}
