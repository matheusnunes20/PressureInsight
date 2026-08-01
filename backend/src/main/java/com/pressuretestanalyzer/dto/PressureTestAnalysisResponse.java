package com.pressuretestanalyzer.dto;

import com.pressuretestanalyzer.parser.PressureRecord;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

/**
 * Saida do endpoint de analise. Todos os valores vem diretamente do
 * {@link AnalysisResult} ja calculado pelo {@code PressureDropAnalysisService}
 * e da lista de pontos destacados do {@code ChartService}; este record so
 * remonta o formato de resposta, sem recalcular nada.
 */
public record PressureTestAnalysisResponse(
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal startPressure,
        BigDecimal endPressure,
        String pressureUnit,
        BigDecimal pressureDrop,
        BigDecimal dropPercentage,
        int durationMinutes,
        BigDecimal maxDropPercentage,
        boolean approved,
        List<PressureRecord> highlightedPoints,
        String chartBase64
) {

    public static PressureTestAnalysisResponse from(AnalysisResult result, List<PressureRecord> highlightedPoints,
                                                      byte[] chartPng) {
        return new PressureTestAnalysisResponse(
                result.startRecord().time(),
                result.endRecord().time(),
                result.startRecord().pressure(),
                result.endRecord().pressure(),
                result.startRecord().unit(),
                result.pressureDrop(),
                result.dropPercentage(),
                result.criteria().durationMinutes(),
                result.criteria().maxDropPercentage(),
                result.approved(),
                highlightedPoints,
                Base64.getEncoder().encodeToString(chartPng));
    }
}
