package com.contractops.api.common.service

import com.contractops.api.common.domain.CostCenter
import com.contractops.api.common.repository.CompanyRepository
import com.contractops.api.common.repository.CostCenterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CostCenterService(
    private val costCenterRepository: CostCenterRepository,
    private val companyRepository: CompanyRepository
) {

    fun findAllByTenant(tenantId: UUID): List<CostCenter> =
        costCenterRepository.findByTenantId(tenantId)

    @Transactional
    fun create(tenantId: UUID, companyId: UUID, branchId: UUID?, name: String, code: String?, description: String?): CostCenter {
        val company = companyRepository.findById(companyId).orElseThrow {
            IllegalArgumentException("Empresa não encontrada")
        }

        val costCenter = CostCenter(
            tenantId = tenantId,
            company = company,
            name = name,
            code = code,
            description = description
        )
        return costCenterRepository.save(costCenter)
    }
}
