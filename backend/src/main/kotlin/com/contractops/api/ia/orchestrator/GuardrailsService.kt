package com.contractops.api.ia.orchestrator

import org.springframework.stereotype.Service

/**
 * Guardrails Service - Fase 5.
 * Responsável por validações de segurança, custo e qualidade antes/depois das chamadas de IA.
 */
@Service
class GuardrailsService {

    data class GuardrailResult(
        val allowed: Boolean,
        val reason: String? = null,
        val estimatedTokens: Int = 0,
        val estimatedCostUsd: Double = 0.0
    )

    /**
     * Valida se o prompt é seguro e dentro dos limites.
     */
    fun validatePrompt(prompt: String, options: Map<String, Any> = emptyMap()): GuardrailResult {
        // Guardrail 1: Tamanho máximo
        if (prompt.length > 12000) {
            return GuardrailResult(false, "Prompt excede o limite de 12.000 caracteres")
        }

        // Guardrail 2: Palavras bloqueadas (exemplo simples)
        val blockedWords = listOf("senha", "token", "chave secreta", "dados sigilosos")
        if (blockedWords.any { prompt.lowercase().contains(it) }) {
            return GuardrailResult(false, "Prompt contém conteúdo potencialmente sensível")
        }

        // Estimativa simples de tokens (1 token ≈ 4 caracteres para português)
        val estimatedTokens = (prompt.length / 3.5).toInt().coerceAtLeast(50)

        // Custo estimado (valores fictícios para stub)
        val costPer1kTokens = 0.002 // exemplo
        val estimatedCost = (estimatedTokens / 1000.0) * costPer1kTokens

        return GuardrailResult(
            allowed = true,
            estimatedTokens = estimatedTokens,
            estimatedCostUsd = estimatedCost
        )
    }

    fun validateResponse(response: String): GuardrailResult {
        // Guardrail simples de saída
        if (response.contains("erro interno") || response.contains("não foi possível")) {
            return GuardrailResult(false, "Resposta de baixa qualidade detectada")
        }
        return GuardrailResult(true)
    }
}