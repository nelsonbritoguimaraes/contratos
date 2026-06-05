package com.contractops.api.ia.agents

import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.stereotype.Service

/**
 * Agente Contábil (Fase 5).
 */
@Service
class ContabilAgent(
    private val orchestrator: AiOrchestrator
) {

    fun sugerirLancamentos(descricaoFato: String, valor: String, contrato: String): String {
        val prompt = """
            Você é um contador especialista em empresas de mão de obra exclusiva.
            
            Fato ocorrido: $descricaoFato
            Valor envolvido: $valor
            Contrato: $contrato
            
            Sugira os lançamentos contábeis mais corretos (débito e crédito), contas prováveis e justificativa.
        """.trimIndent()

        return orchestrator.execute(prompt)
    }
}