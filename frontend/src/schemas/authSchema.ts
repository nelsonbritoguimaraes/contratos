/**
 * authSchema — Validação Zod para autenticação
 *
 * Token JWT: string não vazio com mínimo de 10 caracteres.
 */
import { z } from 'zod'

export const authLoginSchema = z.object({
  token: z
    .string()
    .min(1, 'Token é obrigatório')
    .min(10, 'Token JWT deve ter pelo menos 10 caracteres'),
})

export type AuthLoginForm = z.infer<typeof authLoginSchema>