package com.contractops.api.email.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Mensagem de e-mail recebida/classificada (starter Fase 2).
 * Usado para notificações de órgãos, glosas, medições, etc.
 */
@Entity
@Table(name = "email_messages")
class EmailMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "from_address", length = 200)
    var fromAddress: String? = null,

    @Column(name = "subject", length = 300)
    var subject: String? = null,

    @Column(name = "body", columnDefinition = "TEXT")
    var body: String? = null,

    @Column(name = "received_at")
    var receivedAt: LocalDateTime? = null,

    @Column(name = "classification", length = 50)
    var classification: String? = null,   // GLOSA, NOTIFICACAO_ORGAO, MEDICAO, CONTRATO, OUTRO

    @Column(name = "linked_entity_type", length = 50)
    var linkedEntityType: String? = null,

    @Column(name = "linked_entity_id")
    var linkedEntityId: UUID? = null,

    @Column(name = "status", length = 30)
    var status: String = "RECEBIDO"   // RECEBIDO, CLASSIFICADO, PROCESSADO

) : AuditEntity(), TenantAware {
    override fun toString(): String = "EmailMessage(id=$id, subject='$subject', classification='$classification')"
}