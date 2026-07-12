// Mirrors the backend DTOs (com.pressuretestanalyzer.dto). Kept in sync by hand
// as endpoints are implemented.

export interface PressureReading {
  timestampSeconds: number;
  pressure: number;
}

export interface AcceptanceCriteria {
  maxDropPercentage: number;
  durationMinutes: number;
}

export interface PressureTestResult {
  id: string;
  fileName: string;
  readings: PressureReading[];
  criteria: AcceptanceCriteria;
  dropPercentage: number;
  passed: boolean;
}
