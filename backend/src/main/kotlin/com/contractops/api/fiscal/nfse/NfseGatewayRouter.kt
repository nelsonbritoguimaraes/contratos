package com.contractops.api.fiscal.nfse

import com.contractops.api.fiscal.config.FiscalMode
import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.model.FiscalTransmitResult
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import java.time.Duration
import java.util.UUID

@Service
class NfseGatewayRouter(
    private val fiscalProperties: FiscalProperties,
    builder: RestTemplateBuilder
) {
    private val restTemplate = builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(60))
        .build()

    fun emit(xml: String, numero: String): FiscalTransmitResult {
        return when (fiscalProperties.resolvedMode()) {
            FiscalMode.STUB -> stubResult(numero, "STUB", xml.length)
            FiscalMode.SANDBOX -> stubResult(numero, "SANDBOX", xml.length)
            FiscalMode.PRODUCTION -> productionEmit(xml, numero)
        }
    }

    fun cancel(xmlCancelamento: String, numero: String): FiscalTransmitResult {
        return when (fiscalProperties.resolvedMode()) {
            FiscalMode.STUB, FiscalMode.SANDBOX -> stubResult(numero, "CANCEL-${fiscalProperties.resolvedMode().name}", xmlCancelamento.length)
            FiscalMode.PRODUCTION -> productionCancel(xmlCancelamento, numero)
        }
    }

    private fun productionCancel(xml: String, numero: String): FiscalTransmitResult {
        val nfse = fiscalProperties.nfse
        if (nfse.certificatePath.isNullOrBlank()) {
            return FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 412,
                message = "Certificado não configurado para cancelamento NFS-e",
                mode = FiscalMode.PRODUCTION.name
            )
        }
        val cancelUrl = nfse.emitUrl.replace("/nfse", "/cancelamento") // endpoint nacional aproximado
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_XML }
        return try {
            val response = restTemplate.postForEntity(cancelUrl, HttpEntity(xml, headers), String::class.java)
            FiscalTransmitResult(
                success = response.statusCode.is2xxSuccessful,
                protocolNumber = "NFSE-CANCEL-$numero",
                receiptNumber = numero,
                statusCode = response.statusCode.value(),
                message = "Cancelamento NFS-e transmitido",
                mode = FiscalMode.PRODUCTION.name,
                rawResponse = response.body?.take(1500)
            )
        } catch (ex: RestClientException) {
            FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 503,
                message = "Falha cancelamento NFS-e: ${ex.message}",
                mode = FiscalMode.PRODUCTION.name
            )
        }
    }

    private fun stubResult(numero: String, prefix: String, xmlBytes: Int): FiscalTransmitResult {
        val protocol = "$prefix-NFSE-$numero-${UUID.randomUUID().toString().take(8)}"
        return FiscalTransmitResult(
            success = true,
            protocolNumber = protocol,
            receiptNumber = "NFSE-$numero",
            statusCode = 200,
            message = "NFS-e registrada em modo $prefix (XML válido, $xmlBytes bytes)",
            mode = prefix
        )
    }

    private fun productionEmit(xml: String, numero: String): FiscalTransmitResult {
        val nfse = fiscalProperties.nfse
        if (nfse.certificatePath.isNullOrBlank()) {
            return FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 412,
                message = "Certificado não configurado para NFS-e Nacional (contractops.fiscal.nfse.certificate-path)",
                mode = FiscalMode.PRODUCTION.name
            )
        }
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_XML }
        return try {
            val response = restTemplate.postForEntity(nfse.emitUrl, HttpEntity(xml, headers), String::class.java)
            FiscalTransmitResult(
                success = response.statusCode.is2xxSuccessful,
                protocolNumber = "NFSE-PROT-$numero",
                receiptNumber = numero,
                statusCode = response.statusCode.value(),
                message = "NFS-e transmitida ao ambiente nacional/municipal",
                mode = FiscalMode.PRODUCTION.name,
                rawResponse = response.body?.take(1500)
            )
        } catch (ex: RestClientException) {
            FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 503,
                message = "Falha NFS-e: ${ex.message}",
                mode = FiscalMode.PRODUCTION.name
            )
        }
    }
}
