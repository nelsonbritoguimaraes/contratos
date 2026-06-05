package com.contractops.api.ia.orchestrator.providers

import com.contractops.api.ia.orchestrator.AiProvider
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/** INTEGRATION_STUB: SPEC §23 — OpenAI real via SDK/chave não configurado. */
@Component
@Profile("local", "prod")
class OpenAiProvider : AiProvider {

    override fun getName() = "openai"

    override fun complete(prompt: String, options: Map<String, Any>): String {
        return "[OpenAI Stub] Resposta simulada para: ${prompt.take(70)}...\n\n" +
               "Configure a chave da OpenAI no AiOrchestrator para chamadas reais."
    }

    override fun completeStructured(prompt: String, schema: String, options: Map<String, Any>): String {
        return """{"provider": "openai", "result": "stub", "schema": "$schema"}"""
    }
}