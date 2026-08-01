package com.pressuretestanalyzer.service;

import com.pressuretestanalyzer.chart.ChartService;
import com.pressuretestanalyzer.dto.PressureTestAnalysisRequest;
import com.pressuretestanalyzer.dto.PressureTestAnalysisResponse;
import com.pressuretestanalyzer.exception.InvalidFileFormatException;
import com.pressuretestanalyzer.exception.UnsupportedSensorFileException;
import com.pressuretestanalyzer.parser.SensorFileParserResolver;
import com.pressuretestanalyzer.parser.SensorType;
import com.pressuretestanalyzer.parser.teksensor.TeksensorFileParser;
import com.pressuretestanalyzer.validation.AcceptanceCriteriaValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa o orquestrador ponta a ponta com colaboradores reais (sem mocks),
 * do mesmo jeito que os demais testes de servico deste projeto: um arquivo
 * Teksensor de verdade entra, uma resposta de analise completa sai.
 */
class PressureTestApplicationServiceTest {

    private final PressureIntervalLocator intervalLocator = new PressureIntervalLocator();
    private final PressureTestApplicationService applicationService = new PressureTestApplicationService(
            new SensorFileParserResolver(List.of(new TeksensorFileParser())),
            new PressureDropAnalysisService(intervalLocator, new AcceptanceCriteriaValidator()),
            new ChartService(intervalLocator));

    @Test
    void runsTheFullFlowAndReturnsTheAnalysisResponse() {
        String file = """
                Data;Hora;Pressao (psi)
                01/07/2026;08:00:00;150,00
                01/07/2026;08:05:00;149,50
                01/07/2026;08:10:00;149,00
                01/07/2026;08:15:00;148,50
                """;
        PressureTestAnalysisRequest request = new PressureTestAnalysisRequest(
                "teste.txt",
                file.getBytes(StandardCharsets.UTF_8),
                SensorType.TEKSENSOR,
                LocalTime.of(8, 0, 0),
                15,
                new BigDecimal("2.00"),
                5);

        PressureTestAnalysisResponse response = applicationService.analyze(request);

        assertThat(response.startTime()).isEqualTo(LocalTime.of(8, 0, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(8, 15, 0));
        assertThat(response.startPressure()).isEqualByComparingTo("150.00");
        assertThat(response.endPressure()).isEqualByComparingTo("148.50");
        assertThat(response.pressureUnit()).isEqualTo("psi");
        assertThat(response.pressureDrop()).isEqualByComparingTo("1.50");
        assertThat(response.dropPercentage()).isEqualByComparingTo("1.00");
        assertThat(response.durationMinutes()).isEqualTo(15);
        assertThat(response.maxDropPercentage()).isEqualByComparingTo("2.00");
        assertThat(response.approved()).isTrue();
        assertThat(response.highlightedPoints()).extracting(record -> record.time()).containsExactly(
                LocalTime.of(8, 0, 0),
                LocalTime.of(8, 5, 0),
                LocalTime.of(8, 10, 0),
                LocalTime.of(8, 15, 0));

        byte[] chartPng = Base64.getDecoder().decode(response.chartBase64());
        assertThat(chartPng[0]).isEqualTo((byte) 0x89);
        assertThat(chartPng[1]).isEqualTo((byte) 'P');
        assertThat(chartPng[2]).isEqualTo((byte) 'N');
        assertThat(chartPng[3]).isEqualTo((byte) 'G');
    }

    @Test
    void rejectsWhenDropExceedsTheCriteria() {
        String file = """
                Data;Hora;Pressao (psi)
                01/07/2026;08:00:00;150,00
                01/07/2026;08:15:00;140,00
                """;
        PressureTestAnalysisRequest request = new PressureTestAnalysisRequest(
                "teste.txt",
                file.getBytes(StandardCharsets.UTF_8),
                SensorType.TEKSENSOR,
                LocalTime.of(8, 0, 0),
                15,
                new BigDecimal("2.00"),
                5);

        PressureTestAnalysisResponse response = applicationService.analyze(request);

        assertThat(response.approved()).isFalse();
    }

    @Test
    void throwsWhenTheFileIsEmpty() {
        PressureTestAnalysisRequest request = new PressureTestAnalysisRequest(
                "teste.txt",
                new byte[0],
                SensorType.TEKSENSOR,
                LocalTime.of(8, 0, 0),
                15,
                new BigDecimal("2.00"),
                5);

        assertThatThrownBy(() -> applicationService.analyze(request))
                .isInstanceOf(InvalidFileFormatException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void throwsWhenNoParserSupportsTheFile() {
        PressureTestAnalysisRequest request = new PressureTestAnalysisRequest(
                "teste.txt",
                "conteudo em um formato desconhecido".getBytes(StandardCharsets.UTF_8),
                SensorType.TEKSENSOR,
                LocalTime.of(8, 0, 0),
                15,
                new BigDecimal("2.00"),
                5);

        assertThatThrownBy(() -> applicationService.analyze(request))
                .isInstanceOf(UnsupportedSensorFileException.class);
    }
}
