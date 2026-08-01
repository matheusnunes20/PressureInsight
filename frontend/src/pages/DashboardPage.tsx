import { useState, type ChangeEvent, type FormEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import DownloadIcon from '@mui/icons-material/Download';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import axios from 'axios';
import { analyzePressureTest } from '../services/api';
import type { ApiErrorResponse, PressureTestAnalysisResponse, SensorType } from '../types';

const SENSOR_TYPES: SensorType[] = ['TEKSENSOR'];

function downloadBase64Png(base64: string, fileName: string) {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  const blob = new Blob([bytes], { type: 'image/png' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(err)) {
    if (err.response?.data?.message) {
      return err.response.data.message;
    }
    return 'Nao foi possivel se comunicar com o servidor. Verifique se o backend esta em execucao.';
  }
  return 'Erro inesperado ao analisar o ensaio.';
}

export function DashboardPage() {
  const [file, setFile] = useState<File | null>(null);
  const [sensorType, setSensorType] = useState<SensorType>('TEKSENSOR');
  const [startTime, setStartTime] = useState('08:00:00');
  const [durationMinutes, setDurationMinutes] = useState('15');
  const [maxDropPercentage, setMaxDropPercentage] = useState('1.00');
  const [labelIntervalMinutes, setLabelIntervalMinutes] = useState('5');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<PressureTestAnalysisResponse | null>(null);

  const durationValue = Number(durationMinutes);
  const maxDropValue = Number(maxDropPercentage);
  const labelIntervalValue = Number(labelIntervalMinutes);

  const durationError = durationMinutes !== '' && (!Number.isInteger(durationValue) || durationValue <= 0);
  const maxDropError = maxDropPercentage !== '' && (Number.isNaN(maxDropValue) || maxDropValue < 0);
  const labelIntervalError =
    labelIntervalMinutes !== '' && (!Number.isInteger(labelIntervalValue) || labelIntervalValue <= 0);

  const isFormValid =
    file !== null &&
    startTime !== '' &&
    durationMinutes !== '' &&
    maxDropPercentage !== '' &&
    labelIntervalMinutes !== '' &&
    !durationError &&
    !maxDropError &&
    !labelIntervalError;

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    setFile(event.target.files?.[0] ?? null);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!file || !isFormValid) {
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const response = await analyzePressureTest({
        file,
        sensorType,
        startTime,
        durationMinutes: durationValue,
        maxDropPercentage: maxDropValue,
        labelIntervalMinutes: labelIntervalValue,
      });
      setResult(response);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  function handleDownload() {
    if (!result) {
      return;
    }
    const label = result.startTime.replaceAll(':', '');
    downloadBase64Png(result.chartBase64, `grafico-ensaio-${label}.png`);
  }

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Box>
          <Typography variant="h4" component="h1" sx={{ fontWeight: 600 }}>
            Pressure Test Analyzer
          </Typography>
          <Typography color="text.secondary">
            Envie o arquivo do ensaio e os criterios de aceitacao para gerar a analise e o grafico.
          </Typography>
        </Box>

        <Paper variant="outlined" sx={{ p: 3 }}>
          <Box component="form" onSubmit={handleSubmit}>
            <Stack spacing={2}>
              <Box>
                <Button component="label" variant="outlined" startIcon={<UploadFileIcon />}>
                  Selecionar arquivo .txt
                  <input type="file" accept=".txt,text/plain" hidden onChange={handleFileChange} />
                </Button>
                {file && (
                  <Chip
                    icon={<InsertDriveFileIcon />}
                    label={file.name}
                    onDelete={() => setFile(null)}
                    sx={{ ml: 2 }}
                  />
                )}
              </Box>

              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                  gap: 2,
                }}
              >
                <TextField
                  select
                  label="Sensor"
                  value={sensorType}
                  onChange={(event) => setSensorType(event.target.value as SensorType)}
                >
                  {SENSOR_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </TextField>

                <TextField
                  label="Horario inicial"
                  type="time"
                  value={startTime}
                  onChange={(event) => setStartTime(event.target.value)}
                  slotProps={{ htmlInput: { step: 1 } }}
                />

                <TextField
                  label="Duracao (minutos)"
                  type="number"
                  value={durationMinutes}
                  onChange={(event) => setDurationMinutes(event.target.value)}
                  error={durationError}
                  helperText={durationError ? 'Informe um numero inteiro maior que zero' : ' '}
                  slotProps={{ htmlInput: { min: 1, step: 1 } }}
                />

                <TextField
                  label="Queda maxima permitida (%)"
                  type="number"
                  value={maxDropPercentage}
                  onChange={(event) => setMaxDropPercentage(event.target.value)}
                  error={maxDropError}
                  helperText={maxDropError ? 'Informe um valor maior ou igual a zero' : ' '}
                  slotProps={{ htmlInput: { min: 0, step: 0.01 } }}
                />

                <TextField
                  label="Intervalo dos labels (minutos)"
                  type="number"
                  value={labelIntervalMinutes}
                  onChange={(event) => setLabelIntervalMinutes(event.target.value)}
                  error={labelIntervalError}
                  helperText={labelIntervalError ? 'Informe um numero inteiro maior que zero' : ' '}
                  slotProps={{ htmlInput: { min: 1, step: 1 } }}
                />
              </Box>

              <Box>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={!isFormValid || loading}
                  startIcon={loading ? <CircularProgress size={18} color="inherit" /> : <AnalyticsIcon />}
                >
                  {loading ? 'Analisando...' : 'Analisar'}
                </Button>
              </Box>
            </Stack>
          </Box>
        </Paper>

        {error && <Alert severity="error">{error}</Alert>}

        {result && (
          <Paper variant="outlined" sx={{ p: 3 }}>
            <Stack spacing={3}>
              <Chip
                icon={result.approved ? <CheckCircleIcon /> : <CancelIcon />}
                label={result.approved ? 'APROVADO' : 'REPROVADO'}
                color={result.approved ? 'success' : 'error'}
                sx={{ fontWeight: 700, fontSize: '0.95rem', px: 1, alignSelf: 'flex-start' }}
              />

              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr 1fr', sm: '1fr 1fr 1fr 1fr' },
                  gap: 2,
                }}
              >
                <Stat label="Horario inicial" value={result.startTime} />
                <Stat label="Horario final" value={result.endTime} />
                <Stat
                  label="Pressao inicial"
                  value={`${result.startPressure.toFixed(2)} ${result.pressureUnit}`}
                />
                <Stat
                  label="Pressao final"
                  value={`${result.endPressure.toFixed(2)} ${result.pressureUnit}`}
                />
                <Stat
                  label="Queda absoluta"
                  value={`${result.pressureDrop.toFixed(2)} ${result.pressureUnit}`}
                />
                <Stat
                  label="Queda percentual"
                  value={`${result.dropPercentage.toFixed(2)}%`}
                />
                <Stat label="Duracao" value={`${result.durationMinutes} min`} />
                <Stat label="Limite permitido" value={`${result.maxDropPercentage.toFixed(2)}%`} />
              </Box>

              <Box>
                <Box
                  component="img"
                  src={`data:image/png;base64,${result.chartBase64}`}
                  alt="Grafico Pressao x Tempo do ensaio"
                  sx={{ width: '100%', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}
                />
                <Button
                  variant="outlined"
                  startIcon={<DownloadIcon />}
                  onClick={handleDownload}
                  sx={{ mt: 2 }}
                >
                  Baixar grafico (PNG)
                </Button>
              </Box>
            </Stack>
          </Paper>
        )}
      </Stack>
    </Container>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body1" sx={{ fontWeight: 600 }}>
        {value}
      </Typography>
    </Box>
  );
}
