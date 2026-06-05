package com.contractops.api.ia.orchestrator

import com.contractops.api.ia.orchestrator.providers.*
import com.contractops.api.ia.rag.RagService
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*

/**
 * AI Orchestrator - Coração da Fase 5 (IA Avançada).
 * 
 * Responsabilidades (conforme SPEC §23):
 * - Roteamento entre provedores
 * - Controle de custo e contexto
 * - Guardrails básicos
 * - Logging e auditoria de chamadas
 * - Suporte a RAG (stub por enquanto)
 */
@Service
class AiOrchestrator(
    private val ollamaProvider: OllamaProvider,
    private val openAiProvider: OpenAiProvider,
    private val anthropicProvider: AnthropicProvider,
    private val geminiProvider: GeminiProvider,
    private val ragService: RagService? = null,
    private val guardrailsService: GuardrailsService = GuardrailsService(),
    private val memory: ConversationMemory? = null   // Memória de conversa
) {

    private val defaultProvider: AiProvider = ollamaProvider
    private val callLog = mutableListOf<AiCallLog>()

    data class AiCallLog(
        val timestamp: OffsetDateTime = OffsetDateTime.now(),
        val provider: String,
        val promptPreview: String,
        val responsePreview: String,
        val tenantId: UUID? = null,
        val routedAgents: List<String>? = null,
        val costEstimate: Double? = null
    )

    /**
     * Executa um prompt simples.
     */
    fun execute(prompt: String, options: Map<String, Any> = emptyMap(), tenantId: UUID? = null, sessionId: String? = null): String {
        // Guardrails avançados
        val validation = guardrailsService.validatePrompt(prompt, options)
        if (!validation.allowed) {
            return "Bloqueado pelos guardrails: ${validation.reason}"
        }

        var finalPrompt = prompt

        // Injeta histórico de conversa se existir
        if (sessionId != null && memory != null) {
            val history = memory.getHistory(sessionId)
            if (history.isNotBlank()) {
                finalPrompt = """
                    [Histórico recente da conversa]
                    $history
                    
                    [Nova mensagem]
                    $prompt
                """.trimIndent()
            }
            memory.addMessage(sessionId, "Usuário: $prompt")
        }

        val provider = selectProvider(options)
        val response = provider.complete(finalPrompt, options)

        // Validação de saída
        val outputCheck = guardrailsService.validateResponse(response)
        val finalResponse = if (!outputCheck.allowed) {
            "Resposta filtrada pelos guardrails. Tente reformular a pergunta."
        } else response

        // Guarda resposta no histórico
        if (sessionId != null && memory != null) {
            memory.addMessage(sessionId, "Assistente: ${finalResponse.take(300)}")
        }

        // Logging com custo estimado
        callLog.add(
            AiCallLog(
                provider = provider.getName(),
                promptPreview = finalPrompt.take(120),
                responsePreview = finalResponse.take(120),
                tenantId = tenantId
            )
        )

        println("[IA Cost] Tokens estimados: ${validation.estimatedTokens} | Custo: $${"%.4f".format(validation.estimatedCostUsd)}")

        return finalResponse
    }

    /**
     * Executa prompt pedindo saída estruturada (JSON).
     */
    fun executeStructured(prompt: String, schema: String, options: Map<String, Any> = emptyMap(), tenantId: UUID? = null): String {
        val provider = selectProvider(options)
        val response = provider.completeStructured(prompt, schema, options)

        callLog.add(
            AiCallLog(
                provider = provider.getName(),
                promptPreview = prompt.take(120),
                responsePreview = response.take(120),
                tenantId = tenantId
            )
        )

        return response
    }

    /**
     * Versão com RAG real (busca contexto de contratos, CCTs e documentos).
     */
    fun executeWithRealContext(
        prompt: String,
        contratoId: UUID? = null,
        tenantId: UUID? = null,
        options: Map<String, Any> = emptyMap()
    ): String {
        val context = if (contratoId != null && tenantId != null && ragService != null) {
            ragService.buscarContextoContrato(contratoId, tenantId)
        } else {
            ragService?.buscarContextoGeral(tenantId ?: UUID.randomUUID()) ?: "Nenhum contexto adicional disponível."
        }

        val augmentedPrompt = """
            [CONTEXTO RELEVANTE DO SISTEMA]
            $context
            
            [PERGUNTA / TAREFA]
            $prompt
            
            Responda usando as informações do contexto quando disponíveis.
        """.trimIndent()

        return execute(augmentedPrompt, options, tenantId)
    }

    /**
     * Mantém compatibilidade com versão manual de contexto.
     */
    fun executeWithContext(
        prompt: String,
        context: String,
        options: Map<String, Any> = emptyMap(),
        tenantId: UUID? = null
    ): String {
        val augmentedPrompt = """
            [CONTEXTO DO CONTRATO / DOCUMENTO]
            $context
            
            [PERGUNTA / TAREFA]
            $prompt
            
            Responda usando apenas as informações do contexto quando possível.
        """.trimIndent()

        return execute(augmentedPrompt, options, tenantId)
    }

    private fun selectProvider(options: Map<String, Any>): AiProvider {
        val requested = options["provider"] as? String ?: "ollama"
        return when (requested.lowercase()) {
            "ollama" -> ollamaProvider
            "openai" -> openAiProvider
            "anthropic", "claude" -> anthropicProvider
            "gemini", "google" -> geminiProvider
            else -> defaultProvider
        }
    }

    fun listAvailableProviders(): List<String> = listOf("ollama", "openai", "anthropic", "gemini")

    fun getRecentCalls(limit: Int = 20): List<AiCallLog> = callLog.takeLast(limit)

    /** Registra chamada IA (usado pelo /ask via AgentRouter para histórico real e custos). */
    fun recordIaCall(
        promptPreview: String,
        responsePreview: String,
        provider: String = "router",
        tenantId: UUID? = null,
        routedAgents: List<String>? = null,
        costEstimate: Double? = null
    ) {
        callLog.add(
            AiCallLog(
                provider = provider,
                promptPreview = promptPreview.take(120),
                responsePreview = responsePreview.take(120),
                tenantId = tenantId,
                routedAgents = routedAgents,
                costEstimate = costEstimate
            )
        )
    }

    fun getDashboardSummary(): Map<String, Any> {
        val totalCalls = callLog.size
        val byProvider = callLog.groupBy { it.provider }.mapValues { it.value.size }
        val estimatedTotalCost = callLog.size * 0.0015 // estimativa grosseira

        return mapOf(
            "total_calls" to totalCalls,
            "calls_by_provider" to byProvider,
            "estimated_total_cost_usd" to estimatedTotalCost,
            "last_calls" to callLog.takeLast(5).map {
                mapOf(
                    "time" to it.timestamp.toString(),
                    "provider" to it.provider,
                    "prompt" to it.promptPreview
                )
            }
        )
    }
}