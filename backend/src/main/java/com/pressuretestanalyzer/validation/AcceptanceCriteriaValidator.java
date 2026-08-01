package com.pressuretestanalyzer.validation;

import com.pressuretestanalyzer.model.AcceptanceCriteria;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Decides whether an observed pressure drop meets its acceptance criteria. */
@Component
public class AcceptanceCriteriaValidator {

    public boolean isApproved(BigDecimal dropPercentage, AcceptanceCriteria criteria) {
        return dropPercentage.compareTo(criteria.maxDropPercentage()) <= 0;
    }
}
