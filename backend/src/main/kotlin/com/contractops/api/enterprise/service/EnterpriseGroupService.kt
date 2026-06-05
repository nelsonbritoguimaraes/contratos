package com.contractops.api.enterprise.service

import com.contractops.api.enterprise.domain.EnterpriseGroup
import com.contractops.api.enterprise.repository.EnterpriseGroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EnterpriseGroupService(
    private val repository: EnterpriseGroupRepository
) {
    fun list(tenantId: UUID): List<EnterpriseGroup> =
        repository.findByTenantIdOrderByNameAsc(tenantId)

    fun findById(id: UUID, tenantId: UUID): EnterpriseGroup? =
        repository.findById(id).filter { it.tenantId == tenantId }.orElse(null)

    @Transactional
    fun create(tenantId: UUID, name: String): EnterpriseGroup =
        repository.save(EnterpriseGroup(tenantId = tenantId, name = name))

    @Transactional
    fun update(id: UUID, tenantId: UUID, name: String): EnterpriseGroup? {
        val group = findById(id, tenantId) ?: return null
        group.name = name
        return repository.save(group)
    }
}
