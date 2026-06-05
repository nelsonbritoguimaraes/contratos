package com.contractops.api.ia.orchestrator

/**
 * Interface base para provedores de IA (OpenAI, Anthropic, Gemini, Ollama, etc.).
 * Permite roteamento e abstração no AI Orchestrator.
 */
interface AiProvider {

    fun getName(): String

    /**
     * Envia um prompt e retorna a resposta textual.
     */
    fun complete(prompt: String, options: Map<String, Any> = emptyMap()): String

    /**
     * Versão estruturada (quando o provedor suportar tool calling / structured output).
     */
    fun completeStructured(prompt: String, schema: String, options: Map<String, Any> = emptyMap()): String
}