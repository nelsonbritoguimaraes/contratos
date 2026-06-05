package com.contractops.api.document.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

/**
 * Documento genérico (polimórfico).
 * Pode estar vinculado a Contrato, Funcionário, CCT, Licitação, etc.
 * Parte da Fase 2 + SPEC §11 e §26.
 */
@Entity
@Table(name = "documents")
class Document(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "entity_type", length = 50)
    var entityType: String? = null,   // CONTRACT, EMPLOYEE, CCT, BIDDING, MEASUREMENT, etc.

    @Column(name = "entity_id")
    var entityId: UUID? = null,

    @Column(name = "file_path", length = 500)
    var filePath: String? = null,     // caminho no storage (futuro S3)

    @Column(name = "mime_type", length = 100)
    var mimeType: String? = null,

    @Column(name = "file_size")
    var fileSize: Long? = null,

    @Column(name = "expiry_date")
    var expiryDate: LocalDate? = null,

    @Column(name = "version", length = 20)
    var version: String = "1.0",

    @Column(name = "status", length = 30)
    var status: String = "ACTIVE"     // ACTIVE, EXPIRED, ARCHIVED

) : AuditEntity(), TenantAware {

    override fun toString(): String = "Document(id=$id, title='$title', entityType='$entityType')"
}