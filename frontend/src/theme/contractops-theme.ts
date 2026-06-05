import { createTheme, ThemeOptions } from '@mui/material/styles'

// Material Design 3 inspired theme for ContractOps AI
// Based on SPEC v1.0 section 3.2 — Design System

const md3Tokens = {
  // Primary — Deep indigo (professional, government-adjacent feel)
  primary: {
    main: '#3F2E7D',
    light: '#6659A5',
    dark: '#2A1F54',
    contrastText: '#FFFFFF',
  },
  // Secondary — Teal for operational accents
  secondary: {
    main: '#006D77',
    light: '#4A9B9F',
    dark: '#004D52',
    contrastText: '#FFFFFF',
  },
  // Surface / Background
  background: {
    default: '#F8F9FA',
    paper: '#FFFFFF',
  },
  // Status colors (used in contracts grid)
  success: { main: '#2E7D32' },
  warning: { main: '#ED6C02' },
  error: { main: '#C62828' },
  info: { main: '#0288D1' },
}

const themeOptions: ThemeOptions = {
  palette: {
    mode: 'light',
    primary: md3Tokens.primary,
    secondary: md3Tokens.secondary,
    background: md3Tokens.background,
    success: md3Tokens.success,
    warning: md3Tokens.warning,
    error: md3Tokens.error,
    info: md3Tokens.info,
  },
  typography: {
    fontFamily: "'Roboto', system-ui, -apple-system, sans-serif",
    h1: { fontSize: '2.25rem', fontWeight: 500, letterSpacing: '-0.02em' },
    h2: { fontSize: '1.75rem', fontWeight: 500 },
    h3: { fontSize: '1.5rem', fontWeight: 500 },
    h4: { fontSize: '1.25rem', fontWeight: 500 },
    h5: { fontSize: '1.1rem', fontWeight: 500 },
    h6: { fontSize: '1rem', fontWeight: 500 },
    subtitle1: { fontSize: '0.95rem', fontWeight: 400 },
    body1: { fontSize: '0.95rem' },
    body2: { fontSize: '0.875rem' },
    button: { textTransform: 'none', fontWeight: 500 },
  },
  shape: {
    borderRadius: 12, // MD3 rounded corners
  },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: md3Tokens.primary.main,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
        },
        elevation2: {
          boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: '8px 20px',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          fontWeight: 500,
        },
      },
    },
  },
}

export const contractopsTheme = createTheme(themeOptions)
