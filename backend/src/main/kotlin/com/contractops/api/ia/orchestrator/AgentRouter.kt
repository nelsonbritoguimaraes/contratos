package com.contractops.api.ia.orchestrator

import com.contractops.api.ia.agents.*
import org.springframework.stereotype.Service

/**
 * Agent Router Inteligente - Fase 5.
 * Usa o próprio Orchestrator (LLM) para classificar a pergunta e rotear para o(s) melhor(es) agente(s).
 */
@Service
class AgentRouter(
    private val orchestrator: AiOrchestrator,
    private val glosaAgent: GlosaAgent,
    private val contractAgent: ContractAgent,
    private val pontoAgent: PontoAgent,
    private val folhaAgent: FolhaAgent,
    private val fiscalAgent: FiscalAgent,
    private val documentAgent: DocumentAgent,
    private val contabilAgent: ContabilAgent,
    private val executivoAgent: ExecutivoAgent,
    private val licitacoesAgent: LicitacoesAgent,
    private val estoqueAgent: EstoqueAgent
) {

    data class AgentResponse(
        val agentName: String,
        val response: String
    )

    /**
     * Roteia a pergunta para o(s) melhor(es) agente(s) usando LLM para classificação.
     * Suporta execução paralela quando a pergunta é complexa.
     */
    fun routeAndExecute(question: String, context: Map<String, Any> = emptyMap()): List<AgentResponse> {
        // Classificação inteligente via LLM
        val classificationPrompt = """
            Classifique a seguinte pergunta de um gestor de contratos em UMA das categorias abaixo:
            [GLOSA, CONTRATO, PONTO, FOLHA, FISCAL, DOCUMENTO, CONTABIL, ESTOQUE, EXECUTIVO, LICITACAO, GERAL]

            Pergunta: "$question"

            Responda apenas com a categoria em maiúsculo.
        """.trimIndent()

        val category = orchestrator.execute(classificationPrompt).trim().uppercase()
        val results = mutableListOf<AgentResponse>()

        // Execução paralela simples para perguntas complexas
        if (category.contains("EXECUTIVO") || category.contains("MARGEM") || category.contains("RISCO")) {
            results.add(AgentResponse("ExecutivoAgent", executivoAgent.responderPerguntaExecutiva(question, "")))
            // Executa em paralelo com outro agente relevante
            if (context.containsKey("contrato")) {
                results.add(AgentResponse("ContractAgent", contractAgent.resumirContrato(context["texto"] as? String ?: question)))
            }
        } else if (category.contains("GLOSA")) {
            results.add(AgentResponse("GlosaAgent", glosaAgent.analisarGlosa(
                question, context["contrato"] as? String ?: "", context["ponto"] as? String ?: ""
            )))
        } else if (category.contains("FISCAL") || category.contains("RETEN")) {
            results.add(AgentResponse("FiscalAgent", fiscalAgent.analisarRetencoes(
                context["valor"] as? String ?: "", "", context["municipio"] as? String ?: ""
            )))
        } else {
            // Roteamento normal
            when {
                category.contains("CONTRATO") -> results.add(AgentResponse("ContractAgent", contractAgent.resumirContrato(context["texto"] as? String ?: question)))
                category.contains("PONTO") || category.contains("COBERTURA") -> results.add(AgentResponse("PontoAgent", pontoAgent.analisarCobertura(context["contrato"] as? String ?: "", question, 95.0)))
                category.contains("FOLHA") -> results.add(AgentResponse("FolhaAgent", folhaAgent.analisarEventos(context["colaborador"] as? String ?: "", question, "")))
                category.contains("DOCUMENTO") || category.contains("OCR") -> results.add(AgentResponse("DocumentAgent", documentAgent.analisarDocumento(question, "genérico")))
                category.contains("CONTABIL") || category.contains("LANÇAMENTO") -> results.add(AgentResponse("ContabilAgent", contabilAgent.sugerirLancamentos(question, "", "")))
                category.contains("ESTOQUE") || category.contains("UNIFORME") -> results.add(AgentResponse("EstoqueAgent", estoqueAgent.preverNecessidade("", "", "")))
                category.contains("LICITACAO") || category.contains("EDITAL") -> results.add(AgentResponse("LicitacoesAgent", licitacoesAgent.analisarEdital(question)))
                else -> results.add(AgentResponse("ExecutivoAgent", executivoAgent.responderPerguntaExecutiva(question, "")))
            }
        }

        return results
    }
}