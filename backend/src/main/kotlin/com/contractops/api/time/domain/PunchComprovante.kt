package com.contractops.api.time.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "punch_comprovantes")
class PunchComprovante(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "raw_punch_id")
    val rawPunchId: UUID? = null,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "punch_timestamp", nullable = false)
    val punchTimestamp: LocalDateTime,

    @Column(name = "hash_comprovante", length = 128)
    val hashComprovante: String? = null,

    @Column(name = "conteudo", columnDefinition = "TEXT")
    val conteudo: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
