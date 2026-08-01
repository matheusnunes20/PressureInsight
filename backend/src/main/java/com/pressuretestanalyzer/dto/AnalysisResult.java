package com.pressuretestanalyzer.dto;

import com.pressuretestanalyzer.model.AcceptanceCriteria;
import com.pressuretestanalyzer.parser.PressureRecord;

import java.math.BigDecimal;

public record AnalysisResult(
        PressureRecord startRecord,
        PressureRecord endRecord,
        BigDecimal pressureDrop,
        BigDecimal dropPercentage,
        boolean approved,
        AcceptanceCriteria criteria
) {
}
