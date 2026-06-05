package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.config.ObligationsProperties
import com.contractops.api.fiscal.config.FiscalMode
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@Service
class DctfWebGateway(
    private val obligationsProperties: ObligationsProperties,
    builder: RestTemplateBuilder
) {
    private val restTemplate = builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(60))
        .build()

    fun transmitDeclaracao(competencia: LocalDate, cnpj: String, totalRetencoes: BigDecimal): ObligationTransmitResult {
        val json = """
            {"declaracao":"DCTFWeb","cnpj":"$cnpj","competencia":"$competencia","totalRetencoes":$totalRetencoes}
        """.trimIndent()

        return when (obligationsProperties.resolvedMode()) {
            FiscalMode.STUB, FiscalMode.SANDBOX -> ObligationTransmitResult(
                success = true,
                protocol = "DCTF${competencia.year}${competencia.monthValue.toString().padStart(2, '0')}${cnpj.takeLast(4)}",
                payload = json,
                mode = obligationsProperties.resolvedMode().name,
                message = "DCTFWeb transmitida (sandbox — webservice simulado)"
            )
            FiscalMode.PRODUCTION -> {
                val props = obligationsProperties.dctfweb
                if (props.certificatePath.isNullOrBlank()) {
                    ObligationTransmitResult(
                        success = false,
                        protocol = null,
                        payload = json,
                        mode = "PRODUCTION",
                        message = "Certificado ICP não configurado (contractops.obligations.dctfweb.certificate-path)"
                    )
                } else {
                    try {
                        val response = restTemplate.postForEntity(props.transmitUrl, json, String::class.java)
                        ObligationTransmitResult(
                            success = response.statusCode.is2xxSuccessful,
                            protocol = response.headers.getFirst("X-Recibo") ?: "DCTF-${UUID.randomUUID().toString().take(8)}",
                            payload = response.body ?: json,
                            mode = "PRODUCTION",
                            message = "DCTFWeb HTTP ${response.statusCode.value()}"
                        )
                    } catch (ex: Exception) {
                        ObligationTransmitResult(
                            success = false,
                            protocol = null,
                            payload = json,
                            mode = "PRODUCTION",
                            message = "Falha webservice DCTFWeb: ${ex.message}"
                        )
                    }
                }
            }
        }
    }
}
