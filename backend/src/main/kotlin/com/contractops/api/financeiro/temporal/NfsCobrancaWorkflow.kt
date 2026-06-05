package com.contractops.api.financeiro.temporal

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

/**
 * Workflow longo: NFS-e → DANFSE/e-mail → cobrança (AR) → conciliação.
 * Executado pelo worker Temporal quando `contractops.temporal.enabled=true`.
 */
@WorkflowInterface
interface NfsCobrancaWorkflow {

    @WorkflowMethod
    fun run(input: NfsCobrancaWorkflowInput): NfsCobrancaWorkflowResult
}
