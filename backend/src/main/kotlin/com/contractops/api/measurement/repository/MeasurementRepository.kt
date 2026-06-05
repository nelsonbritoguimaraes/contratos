package com.contractops.api.measurement.repository

import com.contractops.api.measurement.domain.Measurement
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface MeasurementRepository : JpaRepository<Measurement, UUID> {
    fun findByTenantIdAndContractIdAndPeriod(tenantId: UUID, contractId: UUID, period: LocalDate): Measurement?
    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): List<Measurement>
}