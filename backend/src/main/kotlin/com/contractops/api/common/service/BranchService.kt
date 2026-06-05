package com.contractops.api.common.service

import com.contractops.api.common.domain.Branch
import com.contractops.api.common.repository.BranchRepository
import com.contractops.api.common.repository.CompanyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BranchService(
    private val branchRepository: BranchRepository,
    private val companyRepository: CompanyRepository
) {

    fun findAllByTenant(tenantId: UUID): List<Branch> =
        branchRepository.findByTenantId(tenantId)

    @Transactional
    fun create(tenantId: UUID, companyId: UUID, name: String, city: String?, state: String?): Branch {
        val company = companyRepository.findById(companyId).orElseThrow {
            IllegalArgumentException("Empresa não encontrada")
        }

        val branch = Branch(
            tenantId = tenantId,
            company = company,
            name = name,
            city = city,
            state = state
        )
        return branchRepository.save(branch)
    }
}
