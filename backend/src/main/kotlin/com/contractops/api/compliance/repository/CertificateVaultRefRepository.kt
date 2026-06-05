package com.contractops.api.compliance.repository

import com.contractops.api.compliance.domain.CertificateVaultRef
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CertificateVaultRefRepository : JpaRepository<CertificateVaultRef, UUID> {
    fun findByTenantIdAndActiveTrue(tenantId: UUID): List<CertificateVaultRef>
}
