package com.contractops.api.financeiro.temporal

import com.contractops.api.financeiro.repository.FinanceWorkflowRepository
import com.contractops.api.financeiro.service.FinanceWorkflowOrchestratorService
import com.contractops.api.financeiro.service.NfsOrgaoEmailService
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class NfsCobrancaActivitiesImpl(
    private val nfsOrgaoEmailService: NfsOrgaoEmailService,
    private val workflowRepository: FinanceWorkflowRepository
) : NfsCobrancaActivities {

    override fun enviarDanfseAoOrgao(notaFiscalId: String, tenantId: String, orgaoEmail: String?) {
        nfsOrgaoEmailService.enviarDanfseAoOrgao(
            UUID.fromString(notaFiscalId),
            UUID.fromString(tenantId),
            orgaoEmail
        )
    }

    override fun atualizarEstadoWorkflow(
        financeWorkflowId: String,
        tenantId: String,
        estado: String,
        mensagem: String
    ) {
        val wf = workflowRepository.findByIdAndTenantId(UUID.fromString(financeWorkflowId), UUID.fromString(tenantId))
            ?: return
        wf.estadoAtual = estado
        wf.updatedAt = OffsetDateTime.now()
        wf.erro = null
        workflowRepository.save(wf)
    }

    override fun vincularContaReceber(financeWorkflowId: String, tenantId: String, contaAReceberId: String?) {
        val wf = workflowRepository.findByIdAndTenantId(UUID.fromString(financeWorkflowId), UUID.fromString(tenantId))
            ?: return
        wf.contaAReceberId = contaAReceberId?.let { UUID.fromString(it) }
        wf.estadoAtual = FinanceWorkflowOrchestratorService.Estado.AR_COBRANCA.name
        wf.updatedAt = OffsetDateTime.now()
        workflowRepository.save(wf)
    }

    override fun marcarProntoConciliacao(financeWorkflowId: String, tenantId: String) {
        atualizarEstadoWorkflow(
            financeWorkflowId,
            tenantId,
            FinanceWorkflowOrchestratorService.Estado.CONCILIACAO.name,
            "Aguardando conciliação bancária (extrato / Open Finance)"
        )
    }

    override fun marcarWorkflowConcluido(financeWorkflowId: String, tenantId: String) {
        val wf = workflowRepository.findByIdAndTenantId(UUID.fromString(financeWorkflowId), UUID.fromString(tenantId))
            ?: return
        wf.estadoAtual = FinanceWorkflowOrchestratorService.Estado.CONCLUIDO.name
        wf.concluido = true
        wf.updatedAt = OffsetDateTime.now()
        workflowRepository.save(wf)
    }
}
