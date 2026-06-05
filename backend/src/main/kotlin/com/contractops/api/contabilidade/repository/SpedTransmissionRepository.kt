package com.contractops.api.contabilidade.repository

import com.contractops.api.contabilidade.domain.SpedTransmission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SpedTransmissionRepository : JpaRepository<SpedTransmission, UUID> {
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<SpedTransmission>
}
