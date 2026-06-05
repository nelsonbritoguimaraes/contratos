package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.OpenFinanceConsent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OpenFinanceConsentRepository : JpaRepository<OpenFinanceConsent, UUID> {
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<OpenFinanceConsent>
    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<OpenFinanceConsent>
}
