package com.contractops.api.ia.orchestrator.providers

import com.contractops.api.ia.orchestrator.AiProvider
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Stub para Anthropic (Claude).
 */
@Component
@Profile("local", "prod")
class AnthropicProvider : AiProvider {

    override fun getName() = "anthropic"

    override fun complete(prompt: String, options: Map<String, Any>): String {
        return "[Anthropic/Claude Stub] Resposta simulada para: ${prompt.take(70)}...\n\n" +
               "Integração real com Messages API pendente."
    }

    override fun completeStructured(prompt: String, schema: String, options: Map<String, Any>): String {
        return """{"provider": "anthropic", "result": "stub_structured"}"""
    }
}