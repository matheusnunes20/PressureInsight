package com.pressuretestanalyzer.validation;

import com.pressuretestanalyzer.model.AcceptanceCriteria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptanceCriteriaValidatorTest {

    private final AcceptanceCriteriaValidator validator = new AcceptanceCriteriaValidator();
    private final AcceptanceCriteria criteria = new AcceptanceCriteria(new BigDecimal("1.00"), 15);

    @Test
    void approvesWhenDropIsBelowLimit() {
        assertThat(validator.isApproved(new BigDecimal("0.50"), criteria)).isTrue();
    }

    @Test
    void approvesWhenDropEqualsLimit() {
        assertThat(validator.isApproved(new BigDecimal("1.00"), criteria)).isTrue();
    }

    @Test
    void rejectsWhenDropExceedsLimit() {
        assertThat(validator.isApproved(new BigDecimal("1.01"), criteria)).isFalse();
    }
}
