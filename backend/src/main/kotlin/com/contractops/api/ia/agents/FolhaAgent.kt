package com.contractops.api.ia.agents

import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.stereotype.Service

/**
 * Agente de Folha / DP (Fase 5).
 */
@Service
class FolhaAgent(
    private val orchestrator: AiOrchestrator
) {

    fun analisarEventos(
        colaborador: String,
        eventos: String,
        rubricasAplicadas: String
    ): String {
        val prompt = """
            Analise os eventos de DP e rubricas aplicadas para o colaborador $colaborador.
            
            Eventos do período:
            $eventos
            
            Rubricas aplicadas:
            $rubricasAplicadas
            
            Tarefa:
            - Verifique inconsistências (ex: férias + adicional noturno no mesmo mês).
            - Sugira correções ou alertas.
            - Indique se há risco trabalhista ou de eSocial.
        """.trimIndent()

        return orchestrator.execute(prompt)
    }
}