package com.contractops.api.ia.agents

import com.contractops.api.ia.orchestrator.AiOrchestrator
import com.contractops.api.ia.rag.RagService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Agente Executivo / CFO (Fase 5).
 * Usa RagService para trazer contexto financeiro real.
 */
@Service
class ExecutivoAgent(
    private val orchestrator: AiOrchestrator,
    private val ragService: RagService? = null
) {

    fun responderPerguntaExecutiva(
        pergunta: String,
        contextoFinanceiro: String = "",
        contratoId: UUID? = null,
        tenantId: UUID? = null
    ): String {
        var contexto = contextoFinanceiro

        if (contratoId != null && tenantId != null && ragService != null) {
            contexto = ragService.buscarContextoFinanceiro(contratoId, tenantId)
        }

        val prompt = """
            Você é um CFO virtual de uma empresa de serviços com dedicação exclusiva de mão de obra.
            
            Contexto financeiro atual:
            $contexto
            
            Pergunta do gestor:
            "$pergunta"
            
            Responda de forma direta, com números quando possível, e com recomendações práticas.
        """.trimIndent()

        return orchestrator.execute(prompt, tenantId = tenantId)
    }
}