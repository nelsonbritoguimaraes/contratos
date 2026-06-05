package com.contractops.api.common.service

import com.contractops.api.common.domain.Tenant
import com.contractops.api.common.repository.TenantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TenantService(
    private val tenantRepository: TenantRepository
) {

    fun findById(id: UUID): Tenant? = tenantRepository.findById(id).orElse(null)

    fun findBySlug(slug: String): Tenant? = tenantRepository.findBySlug(slug)

    @Transactional
    fun createTenant(name: String, slug: String): Tenant {
        require(!tenantRepository.existsBySlug(slug)) { "Slug '$slug' já está em uso" }

        val tenant = Tenant(
            name = name,
            slug = slug
        )
        return tenantRepository.save(tenant)
    }

    fun getDemoTenant(): Tenant? {
        // Tenant fixo usado durante desenvolvimento (SPEC seção 4)
        return tenantRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")).orElse(null)
    }
}
