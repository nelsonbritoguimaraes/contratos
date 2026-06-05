/**
 * KpiCard — Componente reutilizável de KPI (ajustado)
 * 
 * Melhorias:
 * - Densidade melhorada (p:2 / 2.25)
 * - Valor usa tamanho controlado (não h4 gigante que quebra em 4 colunas)
 * - Tipografia consistente + prevenção de overflow
 * - Trend mais compacto
 */
import { Paper, Typography, Box, Stack } from '@mui/material'
import { TrendingUp, TrendingDown, Minus } from 'lucide-react'
import React from 'react'

interface KpiCardProps {
  title: string
  value: string | number
  subtitle?: string
  trend?: 'up' | 'down' | 'flat'
  trendValue?: string
  color?: 'primary' | 'success' | 'warning' | 'error'
  icon?: React.ReactNode
  onClick?: () => void
}

export function KpiCard({
  title,
  value,
  subtitle,
  trend = 'flat',
  trendValue,
  color = 'primary',
  icon,
  onClick,
}: KpiCardProps) {
  const TrendIcon = trend === 'up' ? TrendingUp : trend === 'down' ? TrendingDown : Minus
  const trendColor =
    trend === 'up' ? 'success.main' : trend === 'down' ? 'error.main' : 'text.secondary'

  return (
    <Paper
      onClick={onClick}
      elevation={1}
      sx={{
        p: { xs: 1.75, sm: 2 },
        borderRadius: 2.5,
        cursor: onClick ? 'pointer' : 'default',
        transition: 'transform 120ms ease, box-shadow 120ms ease',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        '&:hover': onClick ? { transform: 'translateY(-1px)', boxShadow: 2 } : {},
      }}
    >
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" sx={{ flex: 1 }}>
        <Box sx={{ minWidth: 0 }}>
          <Typography
            variant="overline"
            color="text.secondary"
            sx={{ fontWeight: 600, letterSpacing: '0.04em', fontSize: '0.68rem', lineHeight: 1.1 }}
          >
            {title}
          </Typography>

          <Typography
            sx={{
              mt: 0.6,
              fontSize: { xs: '1.35rem', sm: '1.55rem' },
              fontWeight: 700,
              lineHeight: 1.05,
              color: `${color}.main`,
              wordBreak: 'break-word',
            }}
          >
            {value}
          </Typography>

          {subtitle && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ mt: 0.6, display: 'block', lineHeight: 1.25, fontSize: '0.72rem' }}
            >
              {subtitle}
            </Typography>
          )}
        </Box>

        {icon && (
          <Box sx={{ color: `${color}.main`, opacity: 0.75, flexShrink: 0, ml: 1 }}>
            {icon}
          </Box>
        )}
      </Stack>

      {trendValue && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 1.25, pt: 0.75, borderTop: '1px solid', borderColor: 'divider' }}>
          <TrendIcon size={15} />
          <Typography
            variant="caption"
            sx={{ color: trendColor, fontWeight: 600, fontSize: '0.72rem' }}
          >
            {trendValue}
          </Typography>
        </Box>
      )}
    </Paper>
  )
}
