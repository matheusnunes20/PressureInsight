package com.pressuretestanalyzer.chart;

import com.pressuretestanalyzer.dto.AnalysisResult;
import com.pressuretestanalyzer.model.AcceptanceCriteria;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.service.PressureDropAnalysisService;
import com.pressuretestanalyzer.service.PressureIntervalLocator;
import com.pressuretestanalyzer.validation.AcceptanceCriteriaValidator;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeriesCollection;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida que o ChartService: recebe as leituras do ensaio, monta o grafico
 * Pressao x Tempo, destaca os pontos do intervalo analisado (a cada
 * labelIntervalMinutes) com labels de horario/pressao, e exporta tanto
 * BufferedImage quanto PNG.
 */
class ChartServiceTest {

    private static final Path SAMPLE_OUTPUT = Path.of("build/sample-output/pressure-chart-sample.png");
    private static final int LABEL_INTERVAL_MINUTES = 5;

    private final PressureIntervalLocator intervalLocator = new PressureIntervalLocator();
    private final PressureDropAnalysisService analysisService =
            new PressureDropAnalysisService(intervalLocator, new AcceptanceCriteriaValidator());
    private final ChartService chartService = new ChartService(intervalLocator);

    private final LocalDate date = LocalDate.of(2026, 7, 1);

    @Test
    void plotsEveryReadingReceived() {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        JFreeChart chart = chartService.buildChart(records, result, LABEL_INTERVAL_MINUTES);

        TimeSeriesCollection readingsDataset = (TimeSeriesCollection) chart.getXYPlot().getDataset(0);
        assertThat(readingsDataset.getSeries(0).getItemCount()).isEqualTo(records.size());
    }

    @Test
    void generatesAPressureByTimeChart() {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        JFreeChart chart = chartService.buildChart(records, result, LABEL_INTERVAL_MINUTES);
        XYPlot plot = chart.getXYPlot();

        assertThat(chart.getTitle().getText()).isEqualTo("Ensaio de Estanqueidade - Pressao x Tempo");
        assertThat(plot.getDomainAxis().getLabel()).isEqualTo("Horario");
        assertThat(plot.getRangeAxis().getLabel()).isEqualTo("Pressao (psi)");
    }

    @Test
    void highlightsTheCheckpointsOfTheAnalyzedIntervalAtTheGivenLabelInterval() {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        List<PressureRecord> checkpoints = chartService.resolveCheckpoints(records, result, LABEL_INTERVAL_MINUTES);
        assertThat(checkpoints).extracting(PressureRecord::time).containsExactly(
                LocalTime.of(8, 0, 0),
                LocalTime.of(8, 5, 0),
                LocalTime.of(8, 10, 0),
                LocalTime.of(8, 15, 0));

        JFreeChart chart = chartService.buildChart(records, result, LABEL_INTERVAL_MINUTES);
        TimeSeriesCollection checkpointDataset = (TimeSeriesCollection) chart.getXYPlot().getDataset(1);
        assertThat(checkpointDataset.getSeries(0).getItemCount()).isEqualTo(checkpoints.size());
    }

    @Test
    void usesADifferentLabelIntervalWhenRequested() {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        List<PressureRecord> checkpoints = chartService.resolveCheckpoints(records, result, 10);

        assertThat(checkpoints).extracting(PressureRecord::time).containsExactly(
                LocalTime.of(8, 0, 0),
                LocalTime.of(8, 10, 0),
                LocalTime.of(8, 15, 0));
    }

    @Test
    void addsTimeAndPressureLabelsForEachCheckpoint() {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        JFreeChart chart = chartService.buildChart(records, result, LABEL_INTERVAL_MINUTES);
        List<XYAnnotation> annotations = chart.getXYPlot().getAnnotations();

        assertThat(annotations).hasSize(4);
        assertThat(annotations).allSatisfy(annotation -> assertThat(annotation).isInstanceOf(XYTextAnnotation.class));

        List<String> labels = annotations.stream()
                .map(annotation -> ((XYTextAnnotation) annotation).getText())
                .toList();
        assertThat(labels).contains(
                "08:00:00  |  150.00 psi",
                "08:05:00  |  149.50 psi",
                "08:10:00  |  149.00 psi",
                "08:15:00  |  148.50 psi");
    }

    @Test
    void rendersABufferedImageAtTheRequestedResolution() {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        BufferedImage image = chartService.renderImage(records, result, LABEL_INTERVAL_MINUTES, 1200, 700);

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(1200);
        assertThat(image.getHeight()).isEqualTo(700);
    }

    @Test
    void rendersABufferedImageAtTheDefaultReportResolutionWhenNotSpecified() {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        BufferedImage image = chartService.renderImage(records, result, LABEL_INTERVAL_MINUTES);

        assertThat(image.getWidth()).isEqualTo(1800);
        assertThat(image.getHeight()).isEqualTo(1000);
    }

    @Test
    void exportsAValidPngAtTheRequestedResolution() throws IOException {
        List<PressureRecord> records = readings();
        AnalysisResult result = analyze(records, 15);

        byte[] png = chartService.exportPng(records, result, LABEL_INTERVAL_MINUTES, 800, 500);

        assertThat(png).isNotEmpty();
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(decoded.getWidth()).isEqualTo(800);
        assertThat(decoded.getHeight()).isEqualTo(500);
    }

    @Test
    void generatesASampleChartFromFictitiousDataForManualInspection() throws IOException {
        List<PressureRecord> records = List.of(
                record(8, 0, 0, "200.00"),
                record(8, 1, 0, "199.85"),
                record(8, 2, 0, "199.70"),
                record(8, 3, 0, "199.55"),
                record(8, 4, 0, "199.40"),
                record(8, 5, 0, "199.25"),
                record(8, 6, 0, "199.10"),
                record(8, 7, 0, "198.95"),
                record(8, 8, 0, "198.80"),
                record(8, 9, 0, "198.65"),
                record(8, 10, 0, "198.50"),
                record(8, 11, 0, "198.35"),
                record(8, 12, 0, "198.20"),
                record(8, 13, 0, "198.05"),
                record(8, 14, 0, "197.90"),
                record(8, 15, 0, "197.75")
        );
        AnalysisResult result = analyze(records, 15);

        byte[] png = chartService.exportPng(records, result, LABEL_INTERVAL_MINUTES);

        Files.createDirectories(SAMPLE_OUTPUT.getParent());
        Files.write(SAMPLE_OUTPUT, png);

        assertThat(SAMPLE_OUTPUT).exists();
        assertThat(Files.size(SAMPLE_OUTPUT)).isGreaterThan(0);
    }

    private AnalysisResult analyze(List<PressureRecord> records, int durationMinutes) {
        AcceptanceCriteria criteria = new AcceptanceCriteria(new BigDecimal("2.00"), durationMinutes);
        return analysisService.analyze(records, criteria, LocalTime.of(8, 0, 0));
    }

    private List<PressureRecord> readings() {
        return List.of(
                record(8, 0, 0, "150.00"),
                record(8, 3, 0, "149.80"),
                record(8, 5, 0, "149.50"),
                record(8, 8, 0, "149.20"),
                record(8, 10, 0, "149.00"),
                record(8, 13, 0, "148.70"),
                record(8, 15, 0, "148.50")
        );
    }

    private PressureRecord record(int hour, int minute, int second, String pressure) {
        return new PressureRecord(date, LocalTime.of(hour, minute, second), new BigDecimal(pressure), "psi");
    }
}
