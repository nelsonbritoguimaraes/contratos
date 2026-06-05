package com.contractops.api.fiscal.crypto

import com.contractops.api.fiscal.config.FiscalProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Carrega certificado ICP-Brasil A1 (PKCS#12 / .pfx) para assinatura eSocial e CAdES.
 */
@Component
class IcpBrasilKeyStoreLoader(
    private val fiscalProperties: FiscalProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class KeyMaterial(
        val privateKey: PrivateKey,
        val certificate: X509Certificate,
        val alias: String
    )

    fun loadEsocialKeyMaterial(): KeyMaterial? {
        val path = fiscalProperties.esocial.certificatePath?.trim().orEmpty()
        if (path.isBlank()) return null
        return load(path, fiscalProperties.esocial.certificatePassword)
    }

    fun load(path: String, password: String?): KeyMaterial? {
        return try {
            val ks = KeyStore.getInstance("PKCS12")
            FileInputStream(path).use { fis ->
                ks.load(fis, (password ?: "").toCharArray())
            }
            val alias = ks.aliases().toList().firstOrNull { ks.isKeyEntry(it) }
                ?: return null.also { log.warn("Nenhuma chave privada no certificado: {}", path) }
            val key = ks.getKey(alias, (password ?: "").toCharArray()) as PrivateKey
            val cert = ks.getCertificate(alias) as X509Certificate
            KeyMaterial(key, cert, alias)
        } catch (ex: Exception) {
            log.warn("Falha ao carregar certificado {}: {}", path, ex.message)
            null
        }
    }

    fun isEsocialCertificateConfigured(): Boolean =
        !fiscalProperties.esocial.certificatePath.isNullOrBlank()
}
