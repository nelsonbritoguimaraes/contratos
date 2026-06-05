package com.contractops.api.bidding.integration

import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import java.time.Duration

/**
 * Consulta pública PNCP (dados abertos Lei 14.133) — sem credenciamento para leitura.
 * API: https://pncp.gov.br/api/consulta/v1/
 */
@Component
class PncpClient(builder: RestTemplateBuilder) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val rest = builder
        .setConnectTimeout(Duration.ofSeconds(15))
        .setReadTimeout(Duration.ofSeconds(30))
        .build()

    private val baseUrl = "https://pncp.gov.br/api/consulta/v1"

    fun searchContratacoes(termo: String?, cnpjOrgao: String?, pagina: Int = 1): Map<String, Any> {
        return try {
            val params = buildString {
                append("?pagina=$pagina&tamanhoPagina=20")
                if (!termo.isNullOrBlank()) append("&palavraChave=${termo.trim()}")
                if (!cnpjOrgao.isNullOrBlank()) append("&cnpj=${cnpjOrgao.filter { it.isDigit() }}")
            }
            @Suppress("UNCHECKED_CAST")
            rest.getForObject("$baseUrl/contratacoes/publicacao$params", Map::class.java) as Map<String, Any>
        } catch (ex: RestClientException) {
            log.warn("PNCP indisponível: {}", ex.message)
            mapOf(
                "erro" to (ex.message ?: "PNCP indisponível"),
                "modo" to "FALLBACK",
                "data" to emptyList<Any>()
            )
        }
    }
}
