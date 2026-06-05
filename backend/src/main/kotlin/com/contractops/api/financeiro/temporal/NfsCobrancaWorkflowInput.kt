package com.contractops.api.financeiro.temporal

import java.io.Serializable

data class NfsCobrancaWorkflowInput(
    val financeWorkflowId: String,
    val notaFiscalId: String,
    val contaAReceberId: String?,
    val tenantId: String,
    val orgaoEmail: String? = null,
    /** Dias aguardando recebimento antes de marcar conciliação (0 = pula espera) */
    val aguardarRecebimentoDias: Long = 90
) : Serializable

data class NfsCobrancaWorkflowResult(
    val estadoFinal: String,
    val financeWorkflowId: String
) : Serializable
