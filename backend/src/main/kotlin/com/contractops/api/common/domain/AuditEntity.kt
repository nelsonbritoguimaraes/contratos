package com.contractops.api.common.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

/**
 * Base auditável para todas as entidades do sistema.
 * Alinhado com os requisitos de auditoria da SPEC v1.0 (seção 24).
 */
@MappedSuperclass
abstract class AuditEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null
}
