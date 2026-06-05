/**
 * TanStack Query Client — Configuração Enterprise (Fase 0)
 * Stale times, retry policy e error handling globais.
 */
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2, // 2 minutos — bom equilíbrio para CFO / dashboards
      gcTime: 1000 * 60 * 10,   // 10 minutos
      retry: (failureCount, error: any) => {
        // Não retry em 4xx (exceto 429)
        if (error?.status >= 400 && error?.status < 500 && error?.status !== 429) {
          return false
        }
        return failureCount < 2
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 1,
    },
  },
})
