package com.pressuretestanalyzer.model;

import java.math.BigDecimal;

/**
 * Acceptance criteria for a hydrostatic pressure test, e.g. "max 1% drop
 * over 15 minutes". Not JPA-mapped yet; will gain persistence once the
 * configuration API is built.
 */
public record AcceptanceCriteria(BigDecimal maxDropPercentage, int durationMinutes) {
}
