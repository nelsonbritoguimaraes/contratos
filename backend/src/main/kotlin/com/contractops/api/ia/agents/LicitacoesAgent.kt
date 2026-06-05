package com.contractops.api.ia.agents

import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.stereotype.Service

/**
 * Agente de Licitações (Fase 5).
 */
@Service
class LicitacoesAgent(
    private val orchestrator: AiOrchestrator
) {

    fun analisarEdital(textoEdital: String): String {
        val prompt = """
            Analise o seguinte edital de licitação de serviços de mão de obra:
            
            $textoEdital
            
            Extraia:
            - Objeto e escopo
            - Quantitativo de postos por lote
            - Critérios de medição e IMR
            - Principais riscos e cláusulas agressivas
            - Oportunidade de margem estimada
        """.trimIndent()

        return orchestrator.execute(prompt)
    }
}