package com.pressuretestanalyzer.parser.teksensor;

import com.pressuretestanalyzer.exception.InvalidFileFormatException;
import com.pressuretestanalyzer.parser.PressureRecord;
import com.pressuretestanalyzer.parser.RawSensorFile;
import com.pressuretestanalyzer.parser.SensorFileParser;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Teksensor .txt exports.
 *
 * <p>No real sample file was available when this was written, so the layout
 * below is a provisional guess at a common Brazilian data-logger export
 * (semicolon-delimited, comma decimal separator, unit embedded in the header):
 *
 * <pre>
 * (free-form header lines: equipment model, serial number, ...)
 * Data;Hora;Pressao (psi)
 * 01/07/2026;08:00:00;150,25
 * 01/07/2026;08:00:05;150,20
 * </pre>
 *
 * <p>Every format-specific detail is isolated in the constants below so this
 * can be corrected in one place once a real export is available.
 */
@Component
public class TeksensorFileParser implements SensorFileParser {

    private static final String DELIMITER = ";";
    private static final List<String> HEADER_TOKENS = List.of("data", "hora", "pressao");
    private static final Pattern UNIT_PATTERN = Pattern.compile("pressao\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public boolean supports(RawSensorFile file) {
        return findHeaderIndex(file.lines()).isPresent();
    }

    @Override
    public List<PressureRecord> parse(RawSensorFile file) {
        List<String> lines = file.lines();
        int headerIndex = findHeaderIndex(lines)
                .orElseThrow(() -> new InvalidFileFormatException(
                        file.fileName(), 0, "", "cabecalho Data;Hora;Pressao nao encontrado"));

        String unit = extractUnit(lines.get(headerIndex));
        List<PressureRecord> records = new ArrayList<>();

        for (int i = headerIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            records.add(parseLine(file.fileName(), i + 1, line, unit));
        }
        return records;
    }

    private PressureRecord parseLine(String fileName, int lineNumber, String line, String unit) {
        String[] columns = line.split(DELIMITER, -1);
        if (columns.length < 3) {
            throw new InvalidFileFormatException(fileName, lineNumber, line, "esperado 3 colunas (Data;Hora;Pressao)");
        }

        LocalDate date = parseDate(fileName, lineNumber, line, columns[0].trim());
        LocalTime time = parseTime(fileName, lineNumber, line, columns[1].trim());
        BigDecimal pressure = parsePressure(fileName, lineNumber, line, columns[2].trim());

        return new PressureRecord(date, time, pressure, unit);
    }

    private LocalDate parseDate(String fileName, int lineNumber, String line, String raw) {
        try {
            return LocalDate.parse(raw, DATE_FORMAT);
        } catch (RuntimeException e) {
            throw new InvalidFileFormatException(fileName, lineNumber, line, "data invalida: " + raw);
        }
    }

    private LocalTime parseTime(String fileName, int lineNumber, String line, String raw) {
        try {
            return LocalTime.parse(raw, TIME_FORMAT);
        } catch (RuntimeException e) {
            throw new InvalidFileFormatException(fileName, lineNumber, line, "horario invalido: " + raw);
        }
    }

    private BigDecimal parsePressure(String fileName, int lineNumber, String line, String raw) {
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new InvalidFileFormatException(fileName, lineNumber, line, "pressao invalida: " + raw);
        }
    }

    private String extractUnit(String headerLine) {
        Matcher matcher = UNIT_PATTERN.matcher(headerLine);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Optional<Integer> findHeaderIndex(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (matchesHeader(lines.get(i))) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private boolean matchesHeader(String line) {
        String[] columns = line.split(DELIMITER, -1);
        if (columns.length < HEADER_TOKENS.size()) {
            return false;
        }
        for (int i = 0; i < HEADER_TOKENS.size(); i++) {
            if (!normalize(columns[i]).startsWith(HEADER_TOKENS.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }
}
