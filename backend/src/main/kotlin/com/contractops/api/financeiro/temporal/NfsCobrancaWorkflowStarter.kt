package com.contractops.api.financeiro.temporal

import com.contractops.api.financeiro.config.NfsWorkflowProperties
import com.contractops.api.financeiro.config.TemporalProperties
import com.contractops.api.financeiro.domain.FinanceWorkflow
import com.contractops.api.financeiro.service.FinanceWorkflowOrchestratorService
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NfsCobrancaWorkflowStarter(
    private val temporalProperties: TemporalProperties,
    private val nfsWorkflowProperties: NfsWorkflowProperties,
    private val workflowClient: ObjectProvider<WorkflowClient>,
    private val orchestrator: FinanceWorkflowOrchestratorService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isTemporalEnabled(): Boolean = temporalProperties.enabled

    /**
     * Persiste registro local e, se Temporal estiver habilitado, dispara workflow assíncrono.
     */
    fun iniciar(notaFiscalId: UUID, contaAReceberId: UUID?, tenantId: UUID): FinanceWorkflow {
        val wf = orchestrator.iniciar(notaFiscalId, contaAReceberId, tenantId)

        if (!temporalProperties.enabled) {
            return wf
        }

        val client = workflowClient.getIfAvailable()
        if (client == null) {
            log.warn("Temporal habilitado mas WorkflowClient indisponível — usando apenas persistência local")
            return wf
        }

        val temporalWorkflowId = "nfs-cobranca-${wf.id}"
        val options = WorkflowOptions.newBuilder()
            .setTaskQueue(temporalProperties.taskQueue)
            .setWorkflowId(temporalWorkflowId)
            .build()

        val stub = client.newWorkflowStub(NfsCobrancaWorkflow::class.java, options)
        val input = NfsCobrancaWorkflowInput(
            financeWorkflowId = wf.id.toString(),
            notaFiscalId = notaFiscalId.toString(),
            contaAReceberId = contaAReceberId?.toString(),
            tenantId = tenantId.toString(),
            orgaoEmail = nfsWorkflowProperties.orgaoEmailDefault
        )

        WorkflowClient.start(stub::run, input)
        log.info("Workflow Temporal disparado: {}", temporalWorkflowId)
        return wf
    }
}
