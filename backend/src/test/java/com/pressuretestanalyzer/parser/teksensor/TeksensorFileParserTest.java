package com.pressuretestanalyzer.parser.teksensor;

import com.pressuretestanalyzer.exception.InvalidFileFormatException;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.parser.RawSensorFile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeksensorFileParserTest {

    private final TeksensorFileParser parser = new TeksensorFileParser();

    @Test
    void supportsRecognizesTeksensorHeader() {
        RawSensorFile file = new RawSensorFile("test.txt", List.of(
                "Modelo: TK-100",
                "Data;Hora;Pressao (psi)",
                "01/07/2026;08:00:00;150,25"
        ));

        assertThat(parser.supports(file)).isTrue();
    }

    @Test
    void supportsReturnsFalseWhenHeaderIsMissing() {
        RawSensorFile file = new RawSensorFile("test.txt", List.of(
                "Timestamp,Value",
                "2026-07-01T08:00:00,150.25"
        ));

        assertThat(parser.supports(file)).isFalse();
    }

    @Test
    void parseConvertsDataLinesIntoPressureRecords() {
        RawSensorFile file = new RawSensorFile("test.txt", List.of(
                "Modelo: TK-100",
                "Data;Hora;Pressao (psi)",
                "01/07/2026;08:00:00;150,25",
                "",
                "01/07/2026;08:00:05;150,20"
        ));

        List<PressureRecord> records = parser.parse(file);

        assertThat(records).containsExactly(
                new PressureRecord(LocalDate.of(2026, 7, 1), LocalTime.of(8, 0, 0), new BigDecimal("150.25"), "psi"),
                new PressureRecord(LocalDate.of(2026, 7, 1), LocalTime.of(8, 0, 5), new BigDecimal("150.20"), "psi")
        );
    }

    @Test
    void parseThrowsWhenPressureIsNotNumeric() {
        RawSensorFile file = new RawSensorFile("test.txt", List.of(
                "Data;Hora;Pressao (psi)",
                "01/07/2026;08:00:00;abc"
        ));

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(InvalidFileFormatException.class)
                .hasMessageContaining("linha 2");
    }

    @Test
    void parseThrowsWhenDateIsInvalid() {
        RawSensorFile file = new RawSensorFile("test.txt", List.of(
                "Data;Hora;Pressao (psi)",
                "2026-07-01;08:00:00;150,25"
        ));

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(InvalidFileFormatException.class)
                .hasMessageContaining("data invalida");
    }
}
