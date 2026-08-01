package com.pressuretestanalyzer.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record PressureRecord(LocalDate date, LocalTime time, BigDecimal pressure, String unit) {
}
