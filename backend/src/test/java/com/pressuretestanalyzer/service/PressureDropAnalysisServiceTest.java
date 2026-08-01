package com.pressuretestanalyzer.service;

import com.pressuretestanalyzer.dto.AnalysisResult;
import com.pressuretestanalyzer.model.AcceptanceCriteria;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.validation.AcceptanceCriteriaValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PressureDropAnalysisServiceTest {

    private final PressureDropAnalysisService service =
            new PressureDropAnalysisService(new PressureIntervalLocator(), new AcceptanceCriteriaValidator());

    private final LocalDate date = LocalDate.of(2026, 7, 1);

    @Test
    void computesDropPercentageAndApprovesWithinLimit() {
        List<PressureRecord> records = List.of(
                record(8, 0, 0, "150.00"),
                record(8, 5, 0, "149.50"),
                record(8, 10, 0, "149.00"),
                record(8, 15, 0, "148.50")
        );
        AcceptanceCriteria criteria = new AcceptanceCriteria(new BigDecimal("1.00"), 15);

        AnalysisResult result = service.analyze(records, criteria, LocalTime.of(8, 0, 0));

        assertThat(result.startRecord().pressure()).isEqualByComparingTo("150.00");
        assertThat(result.endRecord().pressure()).isEqualByComparingTo("148.50");
        assertThat(result.pressureDrop()).isEqualByComparingTo("1.50");
        assertThat(result.dropPercentage()).isEqualByComparingTo("1.00");
        assertThat(result.approved()).isTrue();
    }

    @Test
    void rejectsWhenDropExceedsCriteria() {
        List<PressureRecord> records = List.of(
                record(8, 0, 0, "150.00"),
                record(8, 15, 0, "145.00")
        );
        AcceptanceCriteria criteria = new AcceptanceCriteria(new BigDecimal("1.00"), 15);

        AnalysisResult result = service.analyze(records, criteria, LocalTime.of(8, 0, 0));

        assertThat(result.approved()).isFalse();
    }

    private PressureRecord record(int hour, int minute, int second, String pressure) {
        return new PressureRecord(date, LocalTime.of(hour, minute, second), new BigDecimal(pressure), "psi");
    }
}
