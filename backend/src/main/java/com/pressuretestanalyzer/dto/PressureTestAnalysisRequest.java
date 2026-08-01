package com.pressuretestanalyzer.dto;

import com.pressuretestanalyzer.parser.SensorType;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Entrada do endpoint de analise, ja desacoplada do multipart/Servlet (o
 * controller le o {@code MultipartFile} e monta este record), para que o
 * {@code PressureTestApplicationService} possa ser testado sem Spring.
 */
public record PressureTestAnalysisRequest(
        String fileName,
        byte[] fileContent,
        SensorType sensorType,
        LocalTime startTime,
        int durationMinutes,
        BigDecimal maxDropPercentage,
        int labelIntervalMinutes
) {
}
