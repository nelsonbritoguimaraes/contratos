package com.contractops.api.fiscal.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.crypto.IcpBrasilKeyStoreLoader
import com.contractops.api.fiscal.sped.SpedTransmitter
import com.contractops.api.contabilidade.service.SpedService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/fiscal")
class FiscalController(
    private val fiscalProperties: FiscalProperties,
    private val keyStoreLoader: IcpBrasilKeyStoreLoader,
    private val spedService: SpedService,
    private val spedTransmitter: SpedTransmitter
) {

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> {
        val mode = fiscalProperties.resolvedMode()
        return ResponseEntity.ok(
            mapOf(
                "mode" to mode.name,
                "esocial" to mapOf(
                    "tpAmb" to fiscalProperties.esocial.tpAmb,
                    "transmitUrl" to fiscalProperties.esocial.transmitUrl,
                    "certificateConfigured" to keyStoreLoader.isEsocialCertificateConfigured(),
                    "xmlSignature" to if (keyStoreLoader.isEsocialCertificateConfigured()) "ICP_BRASIL" else "STUB_ON_TRANSMIT"
                ),
                "nfse" to mapOf(
                    "municipioIbge" to fiscalProperties.nfse.municipioIbge,
                    "emitUrl" to fiscalProperties.nfse.emitUrl,
                    "certificateConfigured" to !fiscalProperties.nfse.certificatePath.isNullOrBlank(),
                    "certificatePasswordConfigured" to !fiscalProperties.nfse.certificatePassword.isNullOrBlank()
                ),
                "sped" to mapOf(
                    "layoutVersion" to fiscalProperties.sped.layoutVersion
                ),
                "message" to "Integração fiscal: STUB=simulado, SANDBOX=validação local, PRODUCTION=HTTP+certificado"
            )
        )
    }

    @PostMapping("/sped/ecd/transmit")
    fun transmitSpedEcd(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val content = spedService.gerarSpedContabilECD(effectiveTenant, LocalDate.parse(inicio), LocalDate.parse(fim))
        val result = spedTransmitter.registerEcdFile(content, "ECD")
        return ResponseEntity.ok(
            mapOf(
                "transmit" to result,
                "previewLines" to content.lines().take(15)
            )
        )
    }
}
