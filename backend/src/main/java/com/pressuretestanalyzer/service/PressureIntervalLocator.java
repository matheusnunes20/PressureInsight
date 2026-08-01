package com.pressuretestanalyzer.service;

import com.pressuretestanalyzer.exception.IntervalNotFoundException;
import com.pressuretestanalyzer.parser.PressureRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Locates the {@link PressureRecord} closest to a requested time. The sensor
 * samples at a fixed rate, so the requested time rarely matches a reading
 * exactly; the nearest reading is used regardless of how far it is.
 */
@Component
public class PressureIntervalLocator {

    public PressureRecord findNearest(List<PressureRecord> records, LocalTime target) {
        return records.stream()
                .min(Comparator.comparing(record -> Duration.between(target, record.time()).abs()))
                .orElseThrow(() -> new IntervalNotFoundException(
                        "Nao ha leituras disponiveis para localizar o horario " + target));
    }
}
