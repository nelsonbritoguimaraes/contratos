/**
 * contractSchema — Validação Zod para contratos
 *
 * numero: string não vazio
 * orgao: string não vazio
 * valorMensal: número positivo
 * vigenciaInicio: string de data
 * vigenciaFim: string de data
 */
import { z } from 'zod'

export const contractSchema = z
  .object({
    numero: z.string().min(1, 'Número do contrato é obrigatório'),
    orgao: z.string().min(1, 'Órgão é obrigatório'),
    valorMensal: z.number().positive('Valor mensal deve ser positivo'),
    vigenciaInicio: z.string().min(1, 'Data de início da vigência é obrigatória'),
    vigenciaFim: z.string().min(1, 'Data de fim da vigência é obrigatória'),
  })
  .refine((data) => !data.vigenciaInicio || !data.vigenciaFim || data.vigenciaFim >= data.vigenciaInicio, {
    message: 'Data de fim deve ser posterior à data de início',
    path: ['vigenciaFim'],
  })

export type ContractForm = z.infer<typeof contractSchema>