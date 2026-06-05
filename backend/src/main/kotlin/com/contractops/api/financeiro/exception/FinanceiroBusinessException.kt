package com.contractops.api.financeiro.exception

/**
 * Exceção específica para regras de negócio do módulo Financeiro (CFO).
 * Usada para violações de fluxo (ex: tentar pagar conta já paga, emitir NFS-e de medição já faturada, etc.).
 */
class FinanceiroBusinessException(message: String) : RuntimeException(message)