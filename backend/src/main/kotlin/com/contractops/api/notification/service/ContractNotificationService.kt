package com.contractops.api.notification.service

import com.contractops.api.notification.domain.ContractNotification
import com.contractops.api.notification.repository.ContractNotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class ContractNotificationService(
    private val repository: ContractNotificationRepository
) {
    fun list(tenantId: UUID, contractId: UUID? = null): List<ContractNotification> =
        if (contractId != null) repository.findByTenantIdAndContractId(tenantId, contractId)
        else repository.findByTenantIdOrderByReceivedAtDesc(tenantId)

    @Transactional
    fun create(notification: ContractNotification): ContractNotification =
        repository.save(notification)

    @Transactional
    fun updateStatus(id: UUID, tenantId: UUID, status: String, respondedAt: LocalDate? = null): ContractNotification {
        val n = repository.findById(id).orElseThrow()
        require(n.tenantId == tenantId)
        n.status = status
        respondedAt?.let { n.respondedAt = it }
        return repository.save(n)
    }
}
