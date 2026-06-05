package com.contractops.api.ia.orchestrator.providers

import com.contractops.api.ia.orchestrator.AiProvider
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Provedor stub para Ollama (local) ou qualquer modelo local.
 * Em produção faria chamadas reais para localhost:11434.
 */
@Component
@Profile("local")
class OllamaProvider : AiProvider {

    override fun getName() = "ollama"

    override fun complete(prompt: String, options: Map<String, Any>): String {
        // Stub avançado - em produção: HttpClient para Ollama API
        return "[Ollama Stub] Resposta para: ${prompt.take(80)}...\n\n" +
               "Sugestão: Configure Ollama localmente ou troque o provedor no AiOrchestrator."
    }

    override fun completeStructured(prompt: String, schema: String, options: Map<String, Any>): String {
        return """{"result": "stub_structured", "prompt_summary": "${prompt.take(60)}"}"""
    }
}