package com.contractops.api.notification.repository

import com.contractops.api.notification.domain.ContractNotification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ContractNotificationRepository : JpaRepository<ContractNotification, UUID> {
    fun findByTenantIdOrderByReceivedAtDesc(tenantId: UUID): List<ContractNotification>
    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): List<ContractNotification>
}
