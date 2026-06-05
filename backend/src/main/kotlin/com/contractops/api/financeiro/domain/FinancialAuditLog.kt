package com.contractops.api.financeiro.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "financial_audit_log")
class FinancialAuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) val tenantId: UUID,
    @Column(name = "entidade_tipo", nullable = false, length = 50) val entidadeTipo: String,
    @Column(name = "entidade_id") val entidadeId: UUID? = null,
    @Column(name = "acao", nullable = false, length = 50) val acao: String,
    @Column(name = "usuario", length = 150) val usuario: String? = null,
    @Column(name = "detalhe", columnDefinition = "TEXT") val detalhe: String? = null,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime = OffsetDateTime.now()
)
