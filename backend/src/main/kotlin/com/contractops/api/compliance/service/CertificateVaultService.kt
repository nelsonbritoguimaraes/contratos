package com.contractops.api.compliance.service

import com.contractops.api.compliance.domain.CertificateVaultRef
import com.contractops.api.compliance.repository.CertificateVaultRefRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Stub do cofre de certificados digitais — conteúdo real fica em Vault externo (SPEC §4).
 */
@Service
class CertificateVaultService(
    private val repository: CertificateVaultRefRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun listActive(tenantId: UUID): List<CertificateVaultRef> =
        repository.findByTenantIdAndActiveTrue(tenantId)

    fun findById(id: UUID, tenantId: UUID): CertificateVaultRef? =
        repository.findById(id).filter { it.tenantId == tenantId }.orElse(null)

    @Transactional
    fun register(ref: CertificateVaultRef): CertificateVaultRef = repository.save(ref)

    /** Stub — integração futura com HashiCorp Vault / AWS Secrets Manager. */
    @Profile("local")
    fun fetchCertificateBytes(vaultPath: String): ByteArray? {
        log.warn("Certificate vault returning null — real vault integration not configured")
        return null
    }

    @Transactional
    fun deactivate(id: UUID, tenantId: UUID): CertificateVaultRef? {
        val ref = findById(id, tenantId) ?: return null
        ref.active = false
        return repository.save(ref)
    }
}
