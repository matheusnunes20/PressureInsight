/**
 * Reads .txt files exported by pressure sensors (Teksensor, Additel, ...)
 * and converts each line into {@link com.pressuretestanalyzer.model} readings.
 * Each manufacturer format implements a common parser contract so new
 * formats can be added without touching the rest of the application.
 */
package com.pressuretestanalyzer.parser;
