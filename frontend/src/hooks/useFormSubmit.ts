import { useState, useCallback } from 'react'
import { useNotification } from '../components/NotificationProvider'

interface UseFormSubmitOptions<T> {
  /** Callback executado após sucesso da submissão */
  onSuccess?: (result: T) => void
  /** Mensagem exibida em caso de sucesso (default: nenhuma) */
  successMessage?: string
  /** Mensagem exibida em caso de erro (default: error.message) */
  errorMessage?: string
}

interface UseFormSubmitReturn<T> {
  /** Função que dispara a submissão (executa o callback com loading/error/notificação) */
  submit: (callback: () => Promise<T>) => Promise<T | undefined>
  /** Indica se uma submissão está em andamento */
  isSubmitting: boolean
  /** Último erro ocorrido (resetado a cada nova submissão) */
  error: Error | null
}

/**
 * Hook genérico que encapsula o padrão try/catch + showNotification + loading state
 * usado por todos os formulários da aplicação.
 *
 * Uso:
 * tsx
 * const { submit, isSubmitting, error } = useFormSubmit({
 *   successMessage: 'Registro salvo com sucesso!',
 *   onSuccess: (result) => navigate(`/detail/${result.id}`),
 * })
 *
 * const handleSubmit = () => submit(() => apiPost('/endpoint', payload))
 *
 */
export function useFormSubmit<T = unknown>(
  options: UseFormSubmitOptions<T> = {}
): UseFormSubmitReturn<T> {
  const { showNotification } = useNotification()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const submit = useCallback(
    async (callback: () => Promise<T>): Promise<T | undefined> => {
      setIsSubmitting(true)
      setError(null)

      try {
        const result = await callback()
        if (options.successMessage) {
          showNotification(options.successMessage, 'success')
        }
        options.onSuccess?.(result)
        return result
      } catch (err: unknown) {
        const errorObj = err instanceof Error ? err : new Error(String(err))
        setError(errorObj)
        showNotification(options.errorMessage ?? errorObj.message, 'error')
        return undefined
      } finally {
        setIsSubmitting(false)
      }
    },
    [options.successMessage, options.errorMessage, options.onSuccess, showNotification]
  )

  return { submit, isSubmitting, error }
}