import axios from 'axios';
import type { PressureTestAnalysisResponse, SensorType } from '../types';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
});

export interface AnalyzePressureTestParams {
  file: File;
  sensorType: SensorType;
  startTime: string;
  durationMinutes: number;
  maxDropPercentage: number;
  labelIntervalMinutes: number;
}

export async function analyzePressureTest(
  params: AnalyzePressureTestParams,
): Promise<PressureTestAnalysisResponse> {
  const formData = new FormData();
  formData.append('file', params.file);
  formData.append('sensorType', params.sensorType);
  formData.append('startTime', params.startTime);
  formData.append('durationMinutes', String(params.durationMinutes));
  formData.append('maxDropPercentage', String(params.maxDropPercentage));
  formData.append('labelIntervalMinutes', String(params.labelIntervalMinutes));

  const response = await api.post<PressureTestAnalysisResponse>(
    '/pressure-tests/analyze',
    formData,
  );
  return response.data;
}
