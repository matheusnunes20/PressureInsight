package com.pressuretestanalyzer.service;

import com.pressuretestanalyzer.dto.AnalysisResult;
import com.pressuretestanalyzer.model.AcceptanceCriteria;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.validation.AcceptanceCriteriaValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;

/**
 * Computes the pressure drop over an operator-defined interval and checks it
 * against the acceptance criteria. The interval end is
 * {@code startTime + criteria.durationMinutes()}; the interval itself is not
 * searched for automatically.
 */
@Service
public class PressureDropAnalysisService {

    private final PressureIntervalLocator intervalLocator;
    private final AcceptanceCriteriaValidator criteriaValidator;

    public PressureDropAnalysisService(PressureIntervalLocator intervalLocator,
                                        AcceptanceCriteriaValidator criteriaValidator) {
        this.intervalLocator = intervalLocator;
        this.criteriaValidator = criteriaValidator;
    }

    public AnalysisResult analyze(List<PressureRecord> records, AcceptanceCriteria criteria, LocalTime startTime) {
        PressureRecord startRecord = intervalLocator.findNearest(records, startTime);

        LocalTime endTime = startTime.plusMinutes(criteria.durationMinutes());
        PressureRecord endRecord = intervalLocator.findNearest(records, endTime);

        BigDecimal pressureDrop = startRecord.pressure().subtract(endRecord.pressure());
        BigDecimal dropPercentage = pressureDrop
                .divide(startRecord.pressure(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        boolean approved = criteriaValidator.isApproved(dropPercentage, criteria);

        return new AnalysisResult(startRecord, endRecord, pressureDrop, dropPercentage, approved, criteria);
    }
}
