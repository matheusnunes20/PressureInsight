package com.pressuretestanalyzer.controller;

import com.pressuretestanalyzer.dto.PressureTestAnalysisResponse;
import com.pressuretestanalyzer.exception.InvalidFileFormatException;
import com.pressuretestanalyzer.exception.UnsupportedSensorFileException;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.service.PressureTestApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa apenas a camada HTTP (roteamento, binding/validacao de parametros e
 * o GlobalExceptionHandler); a orquestracao real ja e coberta por
 * {@code PressureTestApplicationServiceTest}, entao aqui o application
 * service e mockado.
 */
@WebMvcTest(PressureTestController.class)
class PressureTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PressureTestApplicationService applicationService;

    @Test
    void analyzesAndReturnsTheApplicationServiceResponse() throws Exception {
        PressureTestAnalysisResponse response = new PressureTestAnalysisResponse(
                LocalTime.of(8, 0, 0),
                LocalTime.of(8, 15, 0),
                new BigDecimal("150.00"),
                new BigDecimal("148.50"),
                "psi",
                new BigDecimal("1.50"),
                new BigDecimal("1.00"),
                15,
                new BigDecimal("2.00"),
                true,
                List.of(new PressureRecord(LocalDate.of(2026, 7, 1), LocalTime.of(8, 0, 0),
                        new BigDecimal("150.00"), "psi")),
                "iVBORw0KGgo=");
        given(applicationService.analyze(any())).willReturn(response);

        mockMvc.perform(multipart("/api/v1/pressure-tests/analyze")
                        .file(sampleFile())
                        .param("sensorType", "TEKSENSOR")
                        .param("startTime", "08:00:00")
                        .param("durationMinutes", "15")
                        .param("maxDropPercentage", "2.00")
                        .param("labelIntervalMinutes", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.startPressure").value(150.00))
                .andExpect(jsonPath("$.endPressure").value(148.50))
                .andExpect(jsonPath("$.pressureUnit").value("psi"))
                .andExpect(jsonPath("$.highlightedPoints.length()").value(1))
                .andExpect(jsonPath("$.chartBase64").value("iVBORw0KGgo="));
    }

    @Test
    void returnsBadRequestWhenDurationIsNotPositive() throws Exception {
        mockMvc.perform(multipart("/api/v1/pressure-tests/analyze")
                        .file(sampleFile())
                        .param("sensorType", "TEKSENSOR")
                        .param("startTime", "08:00:00")
                        .param("durationMinutes", "0")
                        .param("maxDropPercentage", "2.00")
                        .param("labelIntervalMinutes", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsBadRequestWhenSensorTypeIsUnknown() throws Exception {
        mockMvc.perform(multipart("/api/v1/pressure-tests/analyze")
                        .file(sampleFile())
                        .param("sensorType", "NAO_EXISTE")
                        .param("startTime", "08:00:00")
                        .param("durationMinutes", "15")
                        .param("maxDropPercentage", "2.00")
                        .param("labelIntervalMinutes", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenApplicationServiceRejectsTheFile() throws Exception {
        given(applicationService.analyze(any()))
                .willThrow(new InvalidFileFormatException("teste.txt", 0, "", "arquivo vazio ou nao enviado"));

        mockMvc.perform(multipart("/api/v1/pressure-tests/analyze")
                        .file(sampleFile())
                        .param("sensorType", "TEKSENSOR")
                        .param("startTime", "08:00:00")
                        .param("durationMinutes", "15")
                        .param("maxDropPercentage", "2.00")
                        .param("labelIntervalMinutes", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void returnsBadRequestWhenNoParserSupportsTheFile() throws Exception {
        given(applicationService.analyze(any()))
                .willThrow(new UnsupportedSensorFileException("teste.txt"));

        mockMvc.perform(multipart("/api/v1/pressure-tests/analyze")
                        .file(sampleFile())
                        .param("sensorType", "TEKSENSOR")
                        .param("startTime", "08:00:00")
                        .param("durationMinutes", "15")
                        .param("maxDropPercentage", "2.00")
                        .param("labelIntervalMinutes", "5"))
                .andExpect(status().isBadRequest());
    }

    private MockMultipartFile sampleFile() {
        String content = """
                Data;Hora;Pressao (psi)
                01/07/2026;08:00:00;150,00
                01/07/2026;08:15:00;148,50
                """;
        return new MockMultipartFile("file", "teste.txt", "text/plain", content.getBytes());
    }
}
