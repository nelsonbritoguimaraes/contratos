/**
 * StatusPipeline — Visual de pipeline/fluxo de status (Fase 0)
 * Excelente para NFS-e, Holerites, Medições, Conciliações, etc.
 */
import { Box, Chip, Stack, Typography } from '@mui/material'

interface StatusPipelineProps {
  steps: Array<{ label: string; status: 'done' | 'current' | 'pending' | 'error' }>
  currentIndex?: number
}

export function StatusPipeline({ steps }: StatusPipelineProps) {
  return (
    <Stack direction="row" spacing={1} alignItems="center" sx={{ flexWrap: 'wrap', gap: 1 }}>
      {steps.map((step, index) => {
        const color =
          step.status === 'done'
            ? 'success'
            : step.status === 'current'
            ? 'primary'
            : step.status === 'error'
            ? 'error'
            : 'default'

        return (
          <Box key={index} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Chip
              label={step.label}
              color={color as any}
              variant={step.status === 'current' ? 'filled' : 'outlined'}
              size="small"
              sx={{ fontWeight: 500 }}
            />
            {index < steps.length - 1 && (
              <Typography variant="caption" color="text.disabled">
                →
              </Typography>
            )}
          </Box>
        )
      })}
    </Stack>
  )
}
