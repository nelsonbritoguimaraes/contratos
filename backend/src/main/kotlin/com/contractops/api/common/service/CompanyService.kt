package com.contractops.api.common.service

import com.contractops.api.common.domain.Company
import com.contractops.api.common.repository.CompanyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CompanyService(
    private val companyRepository: CompanyRepository
) {

    fun findAllByTenant(tenantId: UUID): List<Company> =
        companyRepository.findByTenantId(tenantId)

    fun findById(id: UUID): Company? =
        companyRepository.findById(id).orElse(null)

    @Transactional
    fun create(tenantId: UUID, cnpj: String, razaoSocial: String, nomeFantasia: String?): Company {
        val company = Company(
            tenantId = tenantId,
            cnpj = cnpj,
            razaoSocial = razaoSocial,
            nomeFantasia = nomeFantasia
        )
        return companyRepository.save(company)
    }
}
