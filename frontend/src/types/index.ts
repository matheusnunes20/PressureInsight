// Espelha os DTOs do backend (com.pressuretestanalyzer.dto e .parser).
// Mantido em sincronia manualmente conforme os endpoints avancam.

export type SensorType = 'TEKSENSOR';

// com.pressuretestanalyzer.parser.PressureRecord
export interface PressureRecord {
  date: string;
  time: string;
  pressure: number;
  unit: string;
}

// com.pressuretestanalyzer.dto.PressureTestAnalysisResponse
export interface PressureTestAnalysisResponse {
  startTime: string;
  endTime: string;
  startPressure: number;
  endPressure: number;
  pressureUnit: string;
  pressureDrop: number;
  dropPercentage: number;
  durationMinutes: number;
  maxDropPercentage: number;
  approved: boolean;
  highlightedPoints: PressureRecord[];
  chartBase64: string;
}

// com.pressuretestanalyzer.dto.ErrorResponse
export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
}
