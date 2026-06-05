/**
 * Query Keys Factory — TanStack Query v5
 * Centralizado, tipado e reutilizável para todo o frontend enterprise.
 * Seguindo o padrão validado no backend (multi-tenant + modular).
 *
 * Uso:
 *   queryKey: queryKeys.financeiro.cfoDashboard(tenantId)
 *   invalidate: queryClient.invalidateQueries({ queryKey: queryKeys.financeiro.all(tenantId) })
 */

import { UUID } from './types'

export const queryKeys = {
  // ====================== TENANT / GLOBAL ======================
  tenant: {
    current: () => ['tenant', 'current'] as const,
    companies: () => ['tenant', 'companies'] as const,
  },

  // ====================== FINANCEIRO (CFO LITERAL) ======================
  financeiro: {
    all: (tenantId: UUID) => ['financeiro', tenantId] as const,

    // Dashboard & KPIs
    cfoDashboard: (tenantId: UUID, dataCorte?: string) =>
      ['financeiro', tenantId, 'cfo-dashboard', dataCorte] as const,

    // Fluxo de Caixa
    fluxoReal: (tenantId: UUID, inicio: string, fim: string) =>
      ['financeiro', tenantId, 'fluxo-real', inicio, fim] as const,
    fluxoProjetado: (tenantId: UUID, semanas: number = 13, cenario: string = 'BASE') =>
      ['financeiro', tenantId, 'fluxo-projetado', semanas, cenario] as const,

    // Tesouraria
    contasBancarias: (tenantId: UUID) =>
      ['financeiro', tenantId, 'tesouraria', 'contas'] as const,

    // AR / AP
    contasReceber: (tenantId: UUID, filters?: Record<string, any>) =>
      ['financeiro', tenantId, 'receber', filters] as const,
    contasPagar: (tenantId: UUID, filters?: Record<string, any>) =>
      ['financeiro', tenantId, 'pagar', filters] as const,
    agingAR: (tenantId: UUID, dataCorte?: string) =>
      ['financeiro', tenantId, 'aging', 'ar', dataCorte] as const,
    agingAP: (tenantId: UUID, dataCorte?: string) =>
      ['financeiro', tenantId, 'aging', 'ap', dataCorte] as const,

    // NFS-e
    nfs: (tenantId: UUID, filters?: Record<string, any>) =>
      ['financeiro', tenantId, 'nfs', filters] as const,

    // Conciliação
    conciliacao: (tenantId: UUID, contaBancariaId: UUID, inicio: string, fim: string) =>
      ['financeiro', tenantId, 'conciliacao', contaBancariaId, inicio, fim] as const,

    // Calendário & Compliance
    calendarioObrigacoes: (tenantId: UUID, mes: number, ano: number) =>
      ['financeiro', tenantId, 'calendario', mes, ano] as const,

    // Simulações
    simulacao: (tenantId: UUID, params: Record<string, any>) =>
      ['financeiro', tenantId, 'simulacao', params] as const,
  },

  // ====================== IA (10 AGENTS + ROUTER + /ask) ======================
  ia: {
    all: (tenantId: UUID) => ['ia', tenantId] as const,
    providers: () => ['ia', 'providers'] as const,
    ask: (tenantId: UUID, question: string, contextHash?: string) =>
      ['ia', tenantId, 'ask', question, contextHash] as const,
    calls: (tenantId: UUID, limit: number = 20) =>
      ['ia', tenantId, 'calls', limit] as const,
    dashboard: (tenantId: UUID) => ['ia', tenantId, 'dashboard'] as const,

    // Agentes específicos (para chamadas diretas)
    agente: (tenantId: UUID, agente: string, payloadHash: string) =>
      ['ia', tenantId, 'agente', agente, payloadHash] as const,
  },

  // ====================== RH / FOLHA ======================
  rh: {
    all: (tenantId: UUID) => ['rh', tenantId] as const,
    dashboard: (tenantId: UUID, competence?: string) =>
      ['rh', tenantId, 'dashboard', competence] as const,
    payslips: (tenantId: UUID, contractId?: UUID, competence?: string) =>
      ['rh', tenantId, 'payslips', contractId, competence] as const,
    esocial: {
      pendentes: (tenantId: UUID) => ['rh', tenantId, 'esocial', 'pendentes'] as const,
      events: (tenantId: UUID, employeeId?: UUID) =>
        ['rh', tenantId, 'esocial', 'events', employeeId] as const,
    },
    rubricas: (tenantId: UUID, activeOnly = true) =>
      ['rh', tenantId, 'rubricas', activeOnly] as const,
    employees: (tenantId: UUID) => ['rh', tenantId, 'employees'] as const,
    encargos: (tenantId: UUID, competence: string) =>
      ['rh', tenantId, 'encargos', competence] as const,
    encargosSummary: (tenantId: UUID, contractId?: UUID, competence?: string) =>
      ['rh', tenantId, 'encargos-summary', contractId, competence] as const,
  },

  // ====================== CONTRATOS / OPERAÇÕES ======================
  contracts: {
    all: (tenantId: UUID, status?: string) => ['contracts', tenantId, status] as const,
    detail: (tenantId: UUID, id: UUID) => ['contracts', tenantId, 'detail', id] as const,
    posts: (tenantId: UUID, contractId: UUID) =>
      ['contracts', tenantId, 'posts', contractId] as const,
    amendments: (tenantId: UUID, contractId: UUID) =>
      ['contracts', tenantId, 'amendments', contractId] as const,
  },

  biddings: {
    all: (tenantId: UUID) => ['biddings', tenantId] as const,
    detail: (tenantId: UUID, id: UUID) => ['biddings', tenantId, 'detail', id] as const,
    lots: (tenantId: UUID, biddingId: UUID) =>
      ['biddings', tenantId, 'lots', biddingId] as const,
  },

  measurements: {
    all: (tenantId: UUID, contractId: UUID) =>
      ['measurements', tenantId, contractId] as const,
    detail: (tenantId: UUID, id: UUID) => ['measurements', tenantId, 'detail', id] as const,
  },

  glosas: {
    all: (tenantId: UUID, contractId: UUID, period?: string) =>
      ['glosas', tenantId, contractId, period] as const,
  },

  // ====================== CONTABILIDADE ======================
  contabilidade: {
    all: (tenantId: UUID) => ['contabilidade', tenantId] as const,
    contas: (tenantId: UUID) => ['contabilidade', tenantId, 'contas'] as const,
    lancamentos: (tenantId: UUID, inicio?: string, fim?: string, origemTipo?: string, origemId?: string) =>
      ['contabilidade', tenantId, 'lancamentos', inicio, fim, origemTipo, origemId] as const,
    dre: (tenantId: UUID, contratoId: UUID, inicio: string, fim: string) =>
      ['contabilidade', tenantId, 'dre', contratoId, inicio, fim] as const,
    balancete: (tenantId: UUID, data: string) =>
      ['contabilidade', tenantId, 'balancete', data] as const,
    sped: (tenantId: UUID, tipo: 'ecd' | 'efd-reinf', params: Record<string, any>) =>
      ['contabilidade', tenantId, 'sped', tipo, params] as const,
  },

  // ====================== PONTO ======================
  ponto: {
    all: (tenantId: UUID) => ['ponto', tenantId] as const,
    cobertura: (tenantId: UUID, contractId: UUID, data: string) =>
      ['ponto', tenantId, 'cobertura', contractId, data] as const,
    devices: (tenantId: UUID) => ['ponto', tenantId, 'devices'] as const,
  },
} as const

export type QueryKeys = typeof queryKeys
