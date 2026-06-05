package com.contractops.api.ia.orchestrator.providers

import com.contractops.api.ia.orchestrator.AiProvider
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Stub para Google Gemini.
 */
@Component
@Profile("local", "prod")
class GeminiProvider : AiProvider {

    override fun getName() = "gemini"

    override fun complete(prompt: String, options: Map<String, Any>): String {
        return "[Gemini Stub] Resposta simulada para: ${prompt.take(70)}...\n\n" +
               "Integração com Gemini API pendente."
    }

    override fun completeStructured(prompt: String, schema: String, options: Map<String, Any>): String {
        return """{"provider": "gemini", "structured": true}"""
    }
}