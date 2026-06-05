package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.config.ObligationsProperties
import com.contractops.api.fiscal.config.FiscalMode
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

data class ObligationTransmitResult(
    val success: Boolean,
    val protocol: String?,
    val payload: String,
    val mode: String,
    val message: String
)

@Service
class FgtsDigitalGateway(
    private val obligationsProperties: ObligationsProperties,
    builder: RestTemplateBuilder
) {
    private val restTemplate = builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(60))
        .build()

    fun transmitEventoS1210(
        competencia: LocalDate,
        totalFolha: BigDecimal,
        totalFgts: BigDecimal,
        cnpj: String
    ): ObligationTransmitResult {
        val json = """
            {"evento":"S-1210","competencia":"$competencia","cnpjPrestador":"$cnpj",
            "totalRemuneracao":$totalFolha,"totalFgts":$totalFgts,"status":"GERADO"}
        """.trimIndent()

        return when (obligationsProperties.resolvedMode()) {
            FiscalMode.STUB, FiscalMode.SANDBOX -> ObligationTransmitResult(
                success = true,
                protocol = "FGTS-${competencia}-SANDBOX-${UUID.randomUUID().toString().take(8)}",
                payload = json,
                mode = obligationsProperties.resolvedMode().name,
                message = "Evento S-1210 aceito em modo sandbox (webservice simulado)"
            )
            FiscalMode.PRODUCTION -> productionTransmit(json, "evento-s1210")
        }
    }

    fun transmitGuia(competencia: LocalDate, valorFgts: BigDecimal, cnpj: String): ObligationTransmitResult {
        val body = """{"cnpj":"$cnpj","competencia":"$competencia","valor":$valorFgts}"""
        return when (obligationsProperties.resolvedMode()) {
            FiscalMode.STUB, FiscalMode.SANDBOX -> ObligationTransmitResult(
                success = true,
                protocol = "GUIA-FGTS-${UUID.randomUUID().toString().take(8)}",
                payload = body,
                mode = obligationsProperties.resolvedMode().name,
                message = "Guia FGTS Digital gerada (sandbox)"
            )
            FiscalMode.PRODUCTION -> productionTransmit(body, "guia")
        }
    }

    private fun productionTransmit(body: String, path: String): ObligationTransmitResult {
        val props = obligationsProperties.fgts
        if (props.certificatePath.isNullOrBlank()) {
            return ObligationTransmitResult(
                success = false,
                protocol = null,
                payload = body,
                mode = "PRODUCTION",
                message = "Certificado ICP não configurado (contractops.obligations.fgts.certificate-path)"
            )
        }
        return try {
            val url = "${props.apiUrl.trimEnd('/')}/$path"
            val response = restTemplate.postForEntity(url, body, String::class.java)
            ObligationTransmitResult(
                success = response.statusCode.is2xxSuccessful,
                protocol = response.headers.getFirst("X-Protocol") ?: "FGTS-${UUID.randomUUID().toString().take(8)}",
                payload = response.body ?: body,
                mode = "PRODUCTION",
                message = "Resposta FGTS Digital HTTP ${response.statusCode.value()}"
            )
        } catch (ex: Exception) {
            ObligationTransmitResult(
                success = false,
                protocol = null,
                payload = body,
                mode = "PRODUCTION",
                message = "Falha webservice FGTS: ${ex.message}"
            )
        }
    }
}
