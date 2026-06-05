package com.contractops.api.post.repository

import com.contractops.api.post.domain.ServicePost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ServicePostRepository : JpaRepository<ServicePost, UUID> {

    fun findByContractId(contractId: UUID): List<ServicePost>

    fun findByTenantId(tenantId: UUID): List<ServicePost>
}
