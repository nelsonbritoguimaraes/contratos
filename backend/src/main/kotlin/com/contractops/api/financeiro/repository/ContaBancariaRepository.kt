package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.ContaBancaria
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ContaBancariaRepository : JpaRepository<ContaBancaria, UUID> {

    fun findByTenantId(tenantId: UUID): List<ContaBancaria>

    fun findByTenantIdAndAtivaTrue(tenantId: UUID): List<ContaBancaria>

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): ContaBancaria?
}