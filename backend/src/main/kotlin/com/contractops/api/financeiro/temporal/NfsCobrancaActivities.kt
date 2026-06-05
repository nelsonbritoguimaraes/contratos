package com.contractops.api.financeiro.temporal

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

@ActivityInterface
interface NfsCobrancaActivities {

    @ActivityMethod
    fun enviarDanfseAoOrgao(notaFiscalId: String, tenantId: String, orgaoEmail: String?)

    @ActivityMethod
    fun atualizarEstadoWorkflow(financeWorkflowId: String, tenantId: String, estado: String, mensagem: String)

    @ActivityMethod
    fun vincularContaReceber(financeWorkflowId: String, tenantId: String, contaAReceberId: String?)

    @ActivityMethod
    fun marcarProntoConciliacao(financeWorkflowId: String, tenantId: String)

    @ActivityMethod
    fun marcarWorkflowConcluido(financeWorkflowId: String, tenantId: String)
}
