package com.contractops.api.ia.agents

import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.stereotype.Service

/**
 * Agente de Contratos (Fase 5 - MVP).
 * 
 * Funções:
 * - Resume contratos
 * - Identifica cláusulas importantes (glosa, reajuste, vigência, garantias)
 * - Sugere riscos
 */
@Service
class ContractAgent(
    private val orchestrator: AiOrchestrator
) {

    fun resumirContrato(textoContrato: String): String {
        val prompt = """
            Você é um especialista em contratos públicos de prestação de serviços com dedicação exclusiva de mão de obra.
            
            Texto do contrato:
            $textoContrato
            
            Tarefa:
            - Resuma em 8-10 bullets os pontos mais importantes.
            - Destaque: vigência, valor, regras de medição/glosa, reajuste/repactuação, garantias, penalidades.
            - Identifique 3 riscos principais.
        """.trimIndent()

        return orchestrator.execute(prompt)
    }

    fun extrairRiscos(textoContrato: String): String {
        val prompt = """
            Analise o contrato abaixo e liste os principais riscos operacionais, trabalhistas e financeiros:
            $textoContrato
        """.trimIndent()

        return orchestrator.execute(prompt)
    }
}