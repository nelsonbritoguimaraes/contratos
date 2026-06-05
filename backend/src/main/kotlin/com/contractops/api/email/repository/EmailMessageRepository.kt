package com.contractops.api.email.repository

import com.contractops.api.email.domain.EmailMessage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EmailMessageRepository : JpaRepository<EmailMessage, UUID> {
    fun findByTenantIdAndClassification(tenantId: UUID, classification: String): List<EmailMessage>
}