/**
 * employeeSchema — Validação Zod para colaboradores
 *
 * fullName: string não vazio, mínimo 3 caracteres
 * cpf: string com formato CPF (11 dígitos ou 000.000.000-00)
 * admissionDate: string de data
 * cargo: string opcional
 * salarioBase: número positivo opcional
 */
import { z } from 'zod'

const cpfRegex = /^\d{3}\.\d{3}\.\d{3}-\d{2}$|^\d{11}$/

export const employeeSchema = z.object({
  fullName: z
    .string()
    .min(1, 'Nome completo é obrigatório')
    .min(3, 'Nome deve ter pelo menos 3 caracteres'),
  cpf: z
    .string()
    .min(1, 'CPF é obrigatório')
    .regex(cpfRegex, 'CPF inválido. Use 000.000.000-00 ou 11 dígitos'),
  admissionDate: z.string().min(1, 'Data de admissão é obrigatória'),
  cargo: z.string().optional(),
  salarioBase: z.number().positive('Salário base deve ser positivo').optional(),
})

export type EmployeeForm = z.infer<typeof employeeSchema>