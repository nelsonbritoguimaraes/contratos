package com.contractops.api.ia.agents

import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.stereotype.Service

/**
 * Agente de Estoque (Uniformes e Equipamentos) - Fase 5.
 */
@Service
class EstoqueAgent(
    private val orchestrator: AiOrchestrator
) {

    fun preverNecessidade(
        contrato: String,
        rotatividadeHistorica: String,
        proximosAdmitidos: String
    ): String {
        val prompt = """
            Preveja a necessidade de uniformes e equipamentos para o contrato $contrato.
            
            Histórico de rotatividade: $rotatividadeHistorica
            Próximas admissões previstas: $proximosAdmitidos
            
            Sugira:
            - Quantidades por tipo de item
            - Momento ideal de compra
            - Risco de falta nos próximos 90 dias
        """.trimIndent()

        return orchestrator.execute(prompt)
    }
}