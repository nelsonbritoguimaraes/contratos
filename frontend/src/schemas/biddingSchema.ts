/**
 * biddingSchema — Validação Zod para licitações
 *
 * orgao: string não vazio
 * editalNumero: string não vazio
 * modalidade: enum de valores válidos
 * dataAbertura: string de data
 */
import { z } from 'zod'

export const biddingModalidadeEnum = z.enum([
  'CONCORRENCIA',
  'TOMADA_DE_PRECOS',
  'PREGAO',
  'PREGAO_ELETRONICO',
  'CONVITE',
  'CONSORCIO',
  'LEILAO',
  'DIALOGO_COMPETITIVO',
  'INEXIGIBILIDADE',
  'DISPENSA',
])

export const biddingSchema = z.object({
  orgao: z.string().min(1, 'Órgão é obrigatório'),
  editalNumero: z.string().min(1, 'Número do edital é obrigatório'),
  modalidade: biddingModalidadeEnum,
  dataAbertura: z.string().min(1, 'Data de abertura é obrigatória'),
})

export type BiddingForm = z.infer<typeof biddingSchema>