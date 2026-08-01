package com.pressuretestanalyzer.service;

import com.pressuretestanalyzer.chart.ChartService;
import com.pressuretestanalyzer.dto.AnalysisResult;
import com.pressuretestanalyzer.dto.PressureTestAnalysisRequest;
import com.pressuretestanalyzer.dto.PressureTestAnalysisResponse;
import com.pressuretestanalyzer.exception.InvalidFileFormatException;
import com.pressuretestanalyzer.model.AcceptanceCriteria;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.parser.RawSensorFile;
import com.pressuretestanalyzer.parser.SensorFileParser;
import com.pressuretestanalyzer.parser.SensorFileParserResolver;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Orquestra o fluxo de analise de um ensaio de pressao ponta a ponta:
 * resolve o parser, converte o arquivo em leituras, roda a analise de queda
 * de pressao e gera o grafico. Nenhuma regra de negocio vive aqui - cada
 * etapa delega para a classe que ja a implementa.
 *
 * <p>{@code request.sensorType()} e aceito e validado na entrada, mas nao e
 * usado para escolher o parser: essa selecao continua estrutural, via
 * {@link SensorFileParserResolver}, para nao duplicar aquela logica.
 */
@Service
public class PressureTestApplicationService {

    private final SensorFileParserResolver parserResolver;
    private final PressureDropAnalysisService analysisService;
    private final ChartService chartService;

    public PressureTestApplicationService(SensorFileParserResolver parserResolver,
                                           PressureDropAnalysisService analysisService,
                                           ChartService chartService) {
        this.parserResolver = parserResolver;
        this.analysisService = analysisService;
        this.chartService = chartService;
    }

    public PressureTestAnalysisResponse analyze(PressureTestAnalysisRequest request) {
        requireNonEmptyFile(request);

        RawSensorFile rawFile = RawSensorFile.from(
                request.fileName(), new ByteArrayInputStream(request.fileContent()));
        SensorFileParser parser = parserResolver.resolve(rawFile);
        List<PressureRecord> records = parser.parse(rawFile);

        AcceptanceCriteria criteria = new AcceptanceCriteria(request.maxDropPercentage(), request.durationMinutes());
        AnalysisResult result = analysisService.analyze(records, criteria, request.startTime());

        List<PressureRecord> highlightedPoints =
                chartService.resolveCheckpoints(records, result, request.labelIntervalMinutes());
        byte[] chartPng = chartService.exportPng(records, result, request.labelIntervalMinutes());

        return PressureTestAnalysisResponse.from(result, highlightedPoints, chartPng);
    }

    private void requireNonEmptyFile(PressureTestAnalysisRequest request) {
        if (request.fileContent() == null || request.fileContent().length == 0) {
            throw new InvalidFileFormatException(request.fileName(), 0, "", "arquivo vazio ou nao enviado");
        }
    }
}
