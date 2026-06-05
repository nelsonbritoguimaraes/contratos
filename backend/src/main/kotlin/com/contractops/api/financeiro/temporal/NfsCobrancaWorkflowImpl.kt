package com.contractops.api.financeiro.temporal

import com.contractops.api.financeiro.service.FinanceWorkflowOrchestratorService
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class NfsCobrancaWorkflowImpl : NfsCobrancaWorkflow {

    private val activities: NfsCobrancaActivities = Workflow.newActivityStub(
        NfsCobrancaActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(5)
                    .build()
            )
            .build()
    )

    override fun run(input: NfsCobrancaWorkflowInput): NfsCobrancaWorkflowResult {
        activities.atualizarEstadoWorkflow(
            input.financeWorkflowId,
            input.tenantId,
            FinanceWorkflowOrchestratorService.Estado.NFS_EMITIDA.name,
            "Workflow Temporal iniciado"
        )

        activities.enviarDanfseAoOrgao(input.notaFiscalId, input.tenantId, input.orgaoEmail)
        activities.atualizarEstadoWorkflow(
            input.financeWorkflowId,
            input.tenantId,
            FinanceWorkflowOrchestratorService.Estado.EMAIL_ORGAO.name,
            "DANFSE enviado ao órgão"
        )

        activities.vincularContaReceber(input.financeWorkflowId, input.tenantId, input.contaAReceberId)
        activities.atualizarEstadoWorkflow(
            input.financeWorkflowId,
            input.tenantId,
            FinanceWorkflowOrchestratorService.Estado.AGUARDANDO_RECEBIMENTO.name,
            "Cobrança registrada — aguardando recebimento"
        )

        if (input.aguardarRecebimentoDias > 0) {
            Workflow.sleep(Duration.ofDays(input.aguardarRecebimentoDias))
        }

        activities.marcarProntoConciliacao(input.financeWorkflowId, input.tenantId)
        activities.marcarWorkflowConcluido(input.financeWorkflowId, input.tenantId)

        return NfsCobrancaWorkflowResult(
            estadoFinal = FinanceWorkflowOrchestratorService.Estado.CONCLUIDO.name,
            financeWorkflowId = input.financeWorkflowId
        )
    }
}
