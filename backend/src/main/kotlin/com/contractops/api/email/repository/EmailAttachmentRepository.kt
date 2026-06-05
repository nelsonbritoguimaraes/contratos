package com.contractops.api.email.repository

import com.contractops.api.email.domain.EmailAttachment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EmailAttachmentRepository : JpaRepository<EmailAttachment, UUID> {
    fun findByTenantIdAndEmailId(tenantId: UUID, emailId: UUID): List<EmailAttachment>
}
