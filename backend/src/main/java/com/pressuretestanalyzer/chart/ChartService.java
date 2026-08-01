package com.pressuretestanalyzer.chart;

import com.pressuretestanalyzer.dto.AnalysisResult;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.service.PressureIntervalLocator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Gera o grafico Pressao x Tempo de um ensaio: todas as leituras do sensor
 * em uma linha, mais as leituras nos pontos de controle do criterio de
 * aceitacao (0, 5, 10, ... ate a duracao do ensaio) destacadas com marcador
 * e label de "horario / pressao". A imagem resultante e pensada para ser
 * incorporada ao relatorio em PDF.
 */
@Service
public class ChartService {

    private static final int DEFAULT_WIDTH_PX = 1800;
    private static final int DEFAULT_HEIGHT_PX = 1000;

    private static final DateTimeFormatter LABEL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Color READINGS_COLOR = new Color(31, 119, 180);
    private static final Color CHECKPOINT_COLOR = new Color(214, 39, 40);
    private static final Color LABEL_COLOR = new Color(60, 60, 60);

    private final PressureIntervalLocator intervalLocator;

    public ChartService(PressureIntervalLocator intervalLocator) {
        this.intervalLocator = intervalLocator;
    }

    /** Renderiza o grafico na resolucao padrao do relatorio. */
    public BufferedImage renderImage(List<PressureRecord> records, AnalysisResult analysisResult,
                                      int labelIntervalMinutes) {
        return renderImage(records, analysisResult, labelIntervalMinutes, DEFAULT_WIDTH_PX, DEFAULT_HEIGHT_PX);
    }

    public BufferedImage renderImage(List<PressureRecord> records, AnalysisResult analysisResult,
                                      int labelIntervalMinutes, int widthPx, int heightPx) {
        JFreeChart chart = buildChart(records, analysisResult, labelIntervalMinutes);
        return chart.createBufferedImage(widthPx, heightPx);
    }

    /** Renderiza o grafico e o codifica como PNG na resolucao padrao do relatorio. */
    public byte[] exportPng(List<PressureRecord> records, AnalysisResult analysisResult, int labelIntervalMinutes) {
        return exportPng(records, analysisResult, labelIntervalMinutes, DEFAULT_WIDTH_PX, DEFAULT_HEIGHT_PX);
    }

    public byte[] exportPng(List<PressureRecord> records, AnalysisResult analysisResult,
                             int labelIntervalMinutes, int widthPx, int heightPx) {
        BufferedImage image = renderImage(records, analysisResult, labelIntervalMinutes, widthPx, heightPx);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeBufferedImageAsPNG(out, image);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Pacote-privado (sem "private") para que os testes possam inspecionar o
    // JFreeChart montado (datasets, titulo, annotations) sem precisar reimplementar
    // a logica de montagem do grafico.
    JFreeChart buildChart(List<PressureRecord> records, AnalysisResult analysisResult, int labelIntervalMinutes) {
        TimeSeries readings = new TimeSeries("Leituras de pressao");
        for (PressureRecord record : records) {
            readings.addOrUpdate(toSecond(record), record.pressure().doubleValue());
        }

        List<PressureRecord> checkpoints = resolveCheckpoints(records, analysisResult, labelIntervalMinutes);
        TimeSeries checkpointSeries = new TimeSeries("Pontos de controle");
        for (PressureRecord checkpoint : checkpoints) {
            checkpointSeries.addOrUpdate(toSecond(checkpoint), checkpoint.pressure().doubleValue());
        }

        String unit = records.get(0).unit();
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Ensaio de Estanqueidade - Pressao x Tempo",
                "Horario",
                "Pressao (" + unit + ")",
                new TimeSeriesCollection(readings),
                true,
                false,
                false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer readingsRenderer = new XYLineAndShapeRenderer(true, false);
        readingsRenderer.setSeriesPaint(0, READINGS_COLOR);
        readingsRenderer.setSeriesStroke(0, new BasicStroke(2f));
        plot.setRenderer(0, readingsRenderer);

        plot.setDataset(1, new TimeSeriesCollection(checkpointSeries));
        XYLineAndShapeRenderer checkpointRenderer = new XYLineAndShapeRenderer(false, true);
        checkpointRenderer.setSeriesPaint(0, CHECKPOINT_COLOR);
        checkpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-5, -5, 10, 10));
        plot.setRenderer(1, checkpointRenderer);

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));

        for (PressureRecord checkpoint : checkpoints) {
            plot.addAnnotation(buildLabel(checkpoint));
        }

        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        return chart;
    }

    /**
     * Resolve as leituras mais proximas dos pontos de controle do ensaio: os
     * registros de inicio/fim ja calculados na analise, mais cada marca de
     * {@code labelIntervalMinutes} minutos entre eles (ex.: 0, 5, 10, 15 para
     * um ensaio de 15 minutos com intervalo de 5 minutos). Publico porque a
     * camada de API tambem usa esta lista para preencher os "pontos
     * destacados" da resposta, sem duplicar a logica aqui.
     */
    public List<PressureRecord> resolveCheckpoints(List<PressureRecord> records, AnalysisResult analysisResult,
                                                    int labelIntervalMinutes) {
        List<PressureRecord> checkpoints = new ArrayList<>();
        checkpoints.add(analysisResult.startRecord());

        LocalTime startTime = analysisResult.startRecord().time();
        int durationMinutes = analysisResult.criteria().durationMinutes();
        for (int minute = labelIntervalMinutes; minute < durationMinutes; minute += labelIntervalMinutes) {
            checkpoints.add(intervalLocator.findNearest(records, startTime.plusMinutes(minute)));
        }

        checkpoints.add(analysisResult.endRecord());
        return checkpoints;
    }

    private XYTextAnnotation buildLabel(PressureRecord checkpoint) {
        String label = checkpoint.time().format(LABEL_TIME_FORMAT) + "  |  " + formatPressure(checkpoint);
        XYTextAnnotation annotation = new XYTextAnnotation(
                label,
                toSecond(checkpoint).getFirstMillisecond(),
                checkpoint.pressure().doubleValue());
        annotation.setTextAnchor(TextAnchor.BOTTOM_CENTER);
        annotation.setFont(new Font("SansSerif", Font.BOLD, 13));
        annotation.setPaint(LABEL_COLOR);
        return annotation;
    }

    private String formatPressure(PressureRecord record) {
        return record.pressure().setScale(2, RoundingMode.HALF_UP) + " " + record.unit();
    }

    private Second toSecond(PressureRecord record) {
        LocalDateTime dateTime = LocalDateTime.of(record.date(), record.time());
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        return new Second(date);
    }
}
