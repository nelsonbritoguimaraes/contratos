package com.contractops.api.glosa.repository

import com.contractops.api.glosa.domain.Glosa
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface GlosaRepository : JpaRepository<Glosa, UUID> {
    fun findByContractIdAndMeasurementPeriod(contractId: UUID, period: LocalDate): List<Glosa>
    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): List<Glosa>
    fun findByTenantId(tenantId: UUID, pageable: Pageable): Page<Glosa>
    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID, pageable: Pageable): Page<Glosa>
}
