package com.contractops.api.ia.agents

import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.stereotype.Service

/**
 * Agente Fiscal (Fase 5) - Retenções, NFS-e, SPED, etc.
 */
@Service
class FiscalAgent(
    private val orchestrator: AiOrchestrator
) {

    fun analisarRetencoes(
        nfsValor: String,
        retencoesCalculadas: String,
        municipio: String
    ): String {
        val prompt = """
            Analise as retenções tributárias da seguinte NFS-e:
            
            Valor do serviço: $nfsValor
            Retenções calculadas: $retencoesCalculadas
            Município tomador: $municipio
            
            Verifique:
            - Se as alíquotas estão corretas para o município.
            - Se há retenções obrigatórias faltando.
            - Riscos de autuação.
            - Sugestões de otimização fiscal.
        """.trimIndent()

        return orchestrator.execute(prompt)
    }
}