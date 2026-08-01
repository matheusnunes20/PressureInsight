package com.pressuretestanalyzer.service;

import com.pressuretestanalyzer.exception.IntervalNotFoundException;
import com.pressuretestanalyzer.parser.PressureRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PressureIntervalLocatorTest {

    private final PressureIntervalLocator locator = new PressureIntervalLocator();
    private final LocalDate date = LocalDate.of(2026, 7, 1);

    @Test
    void findsExactMatch() {
        List<PressureRecord> records = List.of(
                record(8, 0, 0, "150.25"),
                record(8, 0, 5, "150.20"),
                record(8, 0, 10, "150.15")
        );

        PressureRecord result = locator.findNearest(records, LocalTime.of(8, 0, 5));

        assertThat(result.pressure()).isEqualByComparingTo("150.20");
    }

    @Test
    void findsClosestWhenNoExactMatch() {
        List<PressureRecord> records = List.of(
                record(8, 0, 0, "150.25"),
                record(8, 0, 5, "150.20"),
                record(8, 0, 10, "150.15")
        );

        PressureRecord result = locator.findNearest(records, LocalTime.of(8, 0, 7));

        assertThat(result.time()).isEqualTo(LocalTime.of(8, 0, 5));
    }

    @Test
    void throwsWhenNoRecordsAvailable() {
        assertThatThrownBy(() -> locator.findNearest(List.of(), LocalTime.of(8, 0, 0)))
                .isInstanceOf(IntervalNotFoundException.class);
    }

    private PressureRecord record(int hour, int minute, int second, String pressure) {
        return new PressureRecord(date, LocalTime.of(hour, minute, second), new BigDecimal(pressure), "psi");
    }
}
