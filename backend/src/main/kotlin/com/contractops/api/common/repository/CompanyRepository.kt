package com.contractops.api.common.repository

import com.contractops.api.common.domain.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CompanyRepository : JpaRepository<Company, UUID> {

    fun findByTenantId(tenantId: UUID): List<Company>

    fun findByTenantIdAndCnpj(tenantId: UUID, cnpj: String): Company?
}
