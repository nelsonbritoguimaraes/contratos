package com.contractops.api.contabilidade.domain

import com.contractops.api.common.domain.AuditEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "accounting_periods")
class AccountingPeriod(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "competencia", nullable = false)
    var competencia: LocalDate,

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ABERTO",

    @Column(name = "fechado_em")
    var fechadoEm: OffsetDateTime? = null,

    @Column(name = "fechado_por", length = 150)
    var fechadoPor: String? = null,

    @Column(name = "observacao", columnDefinition = "TEXT")
    var observacao: String? = null
) : AuditEntity()
