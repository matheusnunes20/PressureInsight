package com.pressuretestanalyzer.controller;

import com.pressuretestanalyzer.dto.PressureTestAnalysisRequest;
import com.pressuretestanalyzer.dto.PressureTestAnalysisResponse;
import com.pressuretestanalyzer.parser.SensorType;
import com.pressuretestanalyzer.service.PressureTestApplicationService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalTime;

/** Traduz a requisicao HTTP de analise em uma chamada ao application service. Sem regras de negocio. */
@RestController
@RequestMapping("/api/v1/pressure-tests")
@Validated
public class PressureTestController {

    private final PressureTestApplicationService applicationService;

    public PressureTestController(PressureTestApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping(path = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PressureTestAnalysisResponse analyze(
            @RequestPart("file") MultipartFile file,
            @RequestParam SensorType sensorType,
            @RequestParam LocalTime startTime,
            @RequestParam @Positive int durationMinutes,
            @RequestParam @DecimalMin("0.0") BigDecimal maxDropPercentage,
            @RequestParam @Positive int labelIntervalMinutes) {

        PressureTestAnalysisRequest request = new PressureTestAnalysisRequest(
                file.getOriginalFilename(),
                readBytes(file),
                sensorType,
                startTime,
                durationMinutes,
                maxDropPercentage,
                labelIntervalMinutes);

        return applicationService.analyze(request);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
