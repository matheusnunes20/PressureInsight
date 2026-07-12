import { Container, Typography } from '@mui/material';

export function DashboardPage() {
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" component="h1">
        PressureTestAnalyzer
      </Typography>
      <Typography color="text.secondary">
        Dashboard placeholder — modules will be implemented next.
      </Typography>
    </Container>
  );
}
