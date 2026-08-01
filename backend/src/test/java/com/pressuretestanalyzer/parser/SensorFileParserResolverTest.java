package com.pressuretestanalyzer.parser;

import com.pressuretestanalyzer.exception.UnsupportedSensorFileException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensorFileParserResolverTest {

    private final SensorFileParser matchingParser = fixedSupport(true);
    private final SensorFileParser nonMatchingParser = fixedSupport(false);

    @Test
    void resolveReturnsTheFirstParserThatSupportsTheFile() {
        SensorFileParserResolver resolver = new SensorFileParserResolver(List.of(nonMatchingParser, matchingParser));

        RawSensorFile file = new RawSensorFile("test.txt", List.of());

        assertThat(resolver.resolve(file)).isSameAs(matchingParser);
    }

    @Test
    void resolveThrowsWhenNoParserSupportsTheFile() {
        SensorFileParserResolver resolver = new SensorFileParserResolver(List.of(nonMatchingParser));

        RawSensorFile file = new RawSensorFile("test.txt", List.of());

        assertThatThrownBy(() -> resolver.resolve(file))
                .isInstanceOf(UnsupportedSensorFileException.class)
                .hasMessageContaining("test.txt");
    }

    private SensorFileParser fixedSupport(boolean supports) {
        return new SensorFileParser() {
            @Override
            public boolean supports(RawSensorFile file) {
                return supports;
            }

            @Override
            public List<PressureRecord> parse(RawSensorFile file) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
