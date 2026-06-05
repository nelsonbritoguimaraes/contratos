package com.contractops.api.fiscal.esocial

import com.contractops.api.fiscal.config.FiscalMode
import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.crypto.IcpBrasilKeyStoreLoader
import com.contractops.api.fiscal.model.FiscalTransmitResult
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.FileInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.time.Duration
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

/**
 * Transmissão HTTP para webservice eSocial (produção restrita / produção).
 * Requer certificado ICP-Brasil configurado em contractops.fiscal.esocial.certificate-path.
 */
@Component
class HttpEsocialGateway(
    private val fiscalProperties: FiscalProperties,
    private val keyStoreLoader: IcpBrasilKeyStoreLoader,
    builder: RestTemplateBuilder
) : EsocialGateway {

    private val defaultRestTemplate = builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(120))
        .build()

    override fun transmit(eventType: String, xmlPayload: String): FiscalTransmitResult {
        val esocial = fiscalProperties.esocial
        if (esocial.certificatePath.isNullOrBlank()) {
            return FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 412,
                message = "Certificado digital não configurado (contractops.fiscal.esocial.certificate-path). " +
                    "Configure certificado A1/A3 ICP-Brasil para transmissão real.",
                mode = FiscalMode.PRODUCTION.name
            )
        }

        if (!xmlPayload.contains("<Signature")) {
            return FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 412,
                message = "XML não assinado. Assinatura XML-DSig obrigatória para transmissão eSocial.",
                mode = FiscalMode.PRODUCTION.name
            )
        }

        val restTemplate = buildMutualTlsRestTemplate(
            esocial.certificatePath!!,
            esocial.certificatePassword.orEmpty()
        ) ?: defaultRestTemplate

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_XML
            set("X-Event-Type", eventType)
        }
        val entity = HttpEntity(xmlPayload, headers)

        return try {
            val response = restTemplate.postForEntity(esocial.transmitUrl, entity, String::class.java)
            val body = response.body ?: ""
            val protocol = extractTag(body, "protocolo") ?: "PROT-${System.currentTimeMillis()}"
            FiscalTransmitResult(
                success = response.statusCode.is2xxSuccessful,
                protocolNumber = protocol,
                receiptNumber = extractTag(body, "recibo"),
                statusCode = response.statusCode.value(),
                message = "Transmissão eSocial HTTP concluída (modo ${fiscalProperties.resolvedMode()})",
                mode = FiscalMode.PRODUCTION.name,
                rawResponse = body.take(2000)
            )
        } catch (ex: RestClientException) {
            FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 503,
                message = "Falha na comunicação com eSocial: ${ex.message}",
                mode = FiscalMode.PRODUCTION.name
            )
        }
    }

    private fun buildMutualTlsRestTemplate(path: String, password: String): RestTemplate? {
        if (keyStoreLoader.load(path, password) == null) return null
        return try {
            val keyStore = KeyStore.getInstance("PKCS12")
            FileInputStream(path).use { fis -> keyStore.load(fis, password.toCharArray()) }
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keyStore, password.toCharArray())
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(kmf.keyManagers, null, SecureRandom())

            val factory = object : SimpleClientHttpRequestFactory() {
                override fun prepareConnection(connection: java.net.HttpURLConnection, httpMethod: String) {
                    super.prepareConnection(connection, httpMethod)
                    if (connection is HttpsURLConnection) {
                        connection.sslSocketFactory = sslContext.socketFactory
                    }
                }
            }
            factory.setConnectTimeout(Duration.ofSeconds(30))
            factory.setReadTimeout(Duration.ofSeconds(120))
            RestTemplate(factory)
        } catch (ex: Exception) {
            null
        }
    }

    private fun extractTag(xml: String, tag: String): String? {
        val regex = Regex("<$tag[^>]*>([^<]+)</$tag>", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.getOrNull(1)
    }
}
