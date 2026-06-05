package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.FinanceWorkflow
import com.contractops.api.financeiro.repository.FinanceWorkflowRepository
import com.contractops.api.financeiro.repository.NotaFiscalServicoRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

/**
 * Orquestrador longo NFS-e → cobrança → conciliação.
 * Persistência em `finance_workflows` — API compatível com migração futura para Temporal.io.
 */
@Service
class FinanceWorkflowOrchestratorService(
    private val workflowRepository: FinanceWorkflowRepository,
    private val notaFiscalRepository: NotaFiscalServicoRepository,
    private val nfsOrgaoEmailService: NfsOrgaoEmailService,
    private val objectMapper: ObjectMapper,
    @Lazy private val financeiroService: FinanceiroService
) {

    enum class Estado {
        INICIADO, NFS_EMITIDA, DANFSE_GERADO, EMAIL_ORGAO, AR_COBRANCA, AGUARDANDO_RECEBIMENTO, CONCILIACAO, CONCLUIDO, ERRO
    }

    @Transactional
    fun iniciar(notaFiscalId: UUID, contaAReceberId: UUID?, tenantId: UUID): FinanceWorkflow {
        val wf = FinanceWorkflow(
            tenantId = tenantId,
            notaFiscalId = notaFiscalId,
            contaAReceberId = contaAReceberId,
            estadoAtual = Estado.INICIADO.name,
            historicoJson = "[]"
        )
        return workflowRepository.save(wf)
    }

    @Transactional
    fun avancar(workflowId: UUID, tenantId: UUID): FinanceWorkflow {
        val wf = workflowRepository.findByIdAndTenantId(workflowId, tenantId)
            ?: throw IllegalArgumentException("Workflow não encontrado")
        val historico = mutableListOf<Map<String, String>>()
        try {
            historico += step(wf.estadoAtual, "inicio")

            when (wf.estadoAtual) {
                Estado.INICIADO.name, Estado.NFS_EMITIDA.name -> {
                    val nfId = wf.notaFiscalId ?: throw IllegalStateException("notaFiscalId obrigatório")
                    val nf = notaFiscalRepository.findById(nfId).orElseThrow()
                    nfsOrgaoEmailService.enviarDanfseAoOrgao(nfId, tenantId)
                    wf.estadoAtual = Estado.EMAIL_ORGAO.name
                    historico += step(wf.estadoAtual, "DANFSE e e-mail ao órgão")
                }
                Estado.EMAIL_ORGAO.name, Estado.DANFSE_GERADO.name -> {
                    wf.contaAReceberId?.let { arId ->
                        try {
                            financeiroService.emitirCobranca(tenantId, arId, "PIX")
                            historico += step("COBRANCA_PIX", "Cobrança PIX emitida para AR $arId")
                        } catch (e: Exception) {
                            historico += step("COBRANCA_PIX_ERRO", e.message ?: "falha cobrança")
                        }
                    }
                    wf.estadoAtual = Estado.AR_COBRANCA.name
                    historico += step(wf.estadoAtual, "Conta a receber vinculada + cobrança")
                }
                Estado.AR_COBRANCA.name, Estado.AGUARDANDO_RECEBIMENTO.name -> {
                    wf.estadoAtual = Estado.CONCILIACAO.name
                    historico += step(wf.estadoAtual, "Pronto para conciliação bancária")
                }
                Estado.CONCILIACAO.name -> {
                    wf.estadoAtual = Estado.CONCLUIDO.name
                    wf.concluido = true
                    historico += step(wf.estadoAtual, "Workflow concluído")
                }
                else -> {
                    wf.estadoAtual = Estado.CONCLUIDO.name
                    wf.concluido = true
                }
            }
        } catch (ex: Exception) {
            wf.estadoAtual = Estado.ERRO.name
            wf.erro = ex.message
            historico += mapOf("estado" to Estado.ERRO.name, "msg" to (ex.message ?: "erro"))
        }
        wf.historicoJson = objectMapper.writeValueAsString(historico)
        wf.updatedAt = OffsetDateTime.now()
        return workflowRepository.save(wf)
    }

    fun listarAtivos(tenantId: UUID): List<FinanceWorkflow> =
        workflowRepository.findByTenantIdAndConcluidoFalseOrderByCreatedAtDesc(tenantId)

    private fun step(estado: String, msg: String) =
        mapOf("estado" to estado, "msg" to msg, "ts" to OffsetDateTime.now().toString())
}
