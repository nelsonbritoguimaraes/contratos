package com.contractops.api.ia.agents

import com.contractops.api.glosa.service.GlosaService
import com.contractops.api.ia.config.AiProperties
import com.contractops.api.ia.domain.IaApprovalQueueItem
import com.contractops.api.ia.orchestrator.AiOrchestrator
import com.contractops.api.ia.rag.RagService
import com.contractops.api.ia.service.IaApprovalQueueService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Agente de Glosas — usa RagService e GlosaService em modo production.
 */
@Service
class GlosaAgent(
    private val orchestrator: AiOrchestrator,
    private val aiProperties: AiProperties,
    private val ragService: RagService? = null,
    private val glosaService: GlosaService? = null,
    private val approvalQueueService: IaApprovalQueueService? = null
) {

    fun analisarGlosa(
        glosaDescricao: String,
        contratoContexto: String = "",
        attendanceResumo: String = "",
        contratoId: UUID? = null,
        tenantId: UUID? = null,
        glosaId: UUID? = null
    ): String {
        var contexto = contratoContexto

        if (contratoId != null && tenantId != null && ragService != null) {
            contexto = ragService.buscarContextoContrato(contratoId, tenantId)
        }

        if (aiProperties.isProduction() && glosaId != null && tenantId != null) {
            glosaService?.findById(glosaId, tenantId)?.let { g ->
                    contexto += "\nGlosa status=${g.status} valor=${g.glosaAmount} tipo=${g.glosaType}"
                }
        }

        val prompt = """
            Você é um especialista em glosas de contratos públicos de mão de obra exclusiva.
            
            Contexto do contrato:
            $contexto
            
            Resumo de ponto/cobertura: $attendanceResumo
            
            Glosa identificada: $glosaDescricao
            
            Tarefa: 
            1. Classifique o tipo de glosa.
            2. Explique a causa provável.
            3. Sugira uma defesa técnica ou ação corretiva.
            4. Dê uma nota de risco (baixo/médio/alto).
            
            Responda de forma objetiva e estruturada.
        """.trimIndent()

        val response = orchestrator.execute(prompt, tenantId = tenantId)

        if (aiProperties.isProduction() && tenantId != null && approvalQueueService != null && glosaId != null) {
            approvalQueueService.enqueue(
                IaApprovalQueueItem(
                    tenantId = tenantId,
                    agentName = "GlosaAgent",
                    actionType = "CONTESTAR_GLOSA",
                    entityType = "GLOSA",
                    entityId = glosaId,
                    contractId = contratoId,
                    title = "Ação sugerida para glosa",
                    summary = response.take(500),
                    requestedBy = "GlosaAgent"
                )
            )
        }

        return response
    }

    fun montarDefesa(glosaId: String, detalhes: String, tenantId: UUID? = null): String {
        val prompt = """
            Monte uma defesa formal e técnica para a seguinte glosa:
            ID: $glosaId
            Detalhes: $detalhes
            
            A defesa deve ser educada, baseada em fatos e alinhada com boas práticas de gestão de contratos públicos.
        """.trimIndent()

        return orchestrator.execute(prompt, tenantId = tenantId)
    }
}
