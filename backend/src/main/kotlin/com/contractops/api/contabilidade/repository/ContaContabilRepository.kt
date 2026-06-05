package com.contractops.api.contabilidade.repository

import com.contractops.api.contabilidade.domain.ContaContabil
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ContaContabilRepository : JpaRepository<ContaContabil, UUID> {

    fun findByTenantIdAndAtivaTrue(tenantId: UUID): List<ContaContabil>

    fun findByTenantId(tenantId: UUID): List<ContaContabil>

    fun findByTenantIdAndCodigo(tenantId: UUID, codigo: String): ContaContabil?
}