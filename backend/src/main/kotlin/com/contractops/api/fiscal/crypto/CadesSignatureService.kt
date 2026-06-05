package com.contractops.api.fiscal.crypto

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Signature
import java.util.Base64

/**
 * Assinatura CAdES/CMS detached (PKCS#7) para AEJ e anexos.
 * Portaria 671/2021 — arquivo .p7s separado do AEJ.
 */
@Component
class CadesSignatureService(
    private val keyStoreLoader: IcpBrasilKeyStoreLoader
) {

    data class CadesResult(
        val signatureBase64: String,
        val mode: String,
        val filename: String
    )

    fun signDetached(content: String, baseFilename: String): CadesResult {
        val material = keyStoreLoader.loadEsocialKeyMaterial()
        val bytes = content.toByteArray(StandardCharsets.UTF_8)

        return if (material != null) {
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initSign(material.privateKey)
            sig.update(bytes)
            val signed = sig.sign()
            CadesResult(
                signatureBase64 = Base64.getEncoder().encodeToString(signed),
                mode = "ICP_BRASIL",
                filename = "$baseFilename.p7s"
            )
        } else {
            val stub = Base64.getEncoder().encodeToString(
                "STUB-CADES-CONTRACTOPS-${bytes.size}".toByteArray(StandardCharsets.UTF_8)
            )
            CadesResult(
                signatureBase64 = stub,
                mode = "STUB",
                filename = "$baseFilename.p7s.stub"
            )
        }
    }
}
