package com.contractops.api.fiscal.efdreinf

import com.contractops.api.financeiro.config.ObligationsProperties
import com.contractops.api.fiscal.config.FiscalMode
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

/**
 * EFD-Reinf — retenções previdenciárias em contratos de terceirização (R-2010 / serviços).
 * Alimenta DCTFWeb junto com eSocial (Receita Federal — integração automática pós S-1299).
 */
data class EfdReinfTransmitResult(
    val success: Boolean,
    val protocol: String?,
    val payload: String,
    val mode: String,
    val message: String
)

@Service
class EfdReinfGateway(
    private val obligationsProperties: ObligationsProperties,
    builder: RestTemplateBuilder
) {
    private val restTemplate = builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(60))
        .build()

    fun transmitR2010Servicos(
        competencia: LocalDate,
        cnpjPrestador: String,
        cnpjTomador: String,
        valorServicos: BigDecimal,
        valorRetencaoInss: BigDecimal
    ): EfdReinfTransmitResult {
        val json = """
            {"evento":"R-2010","competencia":"$competencia","cnpjPrestador":"$cnpjPrestador",
            "cnpjTomador":"$cnpjTomador","vlrServicos":$valorServicos,"vlrRetencaoInss":$valorRetencaoInss}
        """.trimIndent()

        return when (obligationsProperties.resolvedMode()) {
            FiscalMode.STUB, FiscalMode.SANDBOX -> EfdReinfTransmitResult(
                success = true,
                protocol = "REINF-R2010-${competencia.year}${competencia.monthValue}-${UUID.randomUUID().toString().take(6)}",
                payload = json,
                mode = obligationsProperties.resolvedMode().name,
                message = "R-2010 registrado (sandbox) — retenção INSS terceirização"
            )
            FiscalMode.PRODUCTION -> {
                val url = obligationsProperties.dctfweb.transmitUrl.replace("dctfweb", "efdreinf")
                try {
                    val response = restTemplate.postForEntity(url, json, String::class.java)
                    EfdReinfTransmitResult(
                        success = response.statusCode.is2xxSuccessful,
                        protocol = response.headers.getFirst("X-Recibo"),
                        payload = response.body ?: json,
                        mode = "PRODUCTION",
                        message = "EFD-Reinf HTTP ${response.statusCode.value()}"
                    )
                } catch (ex: Exception) {
                    EfdReinfTransmitResult(false, null, json, "PRODUCTION", ex.message ?: "erro")
                }
            }
        }
    }
}
