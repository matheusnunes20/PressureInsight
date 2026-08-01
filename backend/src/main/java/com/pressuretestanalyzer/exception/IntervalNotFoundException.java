package com.pressuretestanalyzer.exception;

/** Thrown when a pressure interval cannot be located because there are no readings to search. */
public class IntervalNotFoundException extends RuntimeException {

    public IntervalNotFoundException(String reason) {
        super(reason);
    }
}
