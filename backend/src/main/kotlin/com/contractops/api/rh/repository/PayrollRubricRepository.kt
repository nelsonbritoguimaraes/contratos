package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.PayrollRubric
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PayrollRubricRepository : JpaRepository<PayrollRubric, UUID> {

    fun findByTenantIdAndIsActiveTrue(tenantId: UUID): List<PayrollRubric>

    fun findByTenantIdAndCode(tenantId: UUID, code: String): PayrollRubric?

    fun findByTenantId(tenantId: UUID): List<PayrollRubric>
}