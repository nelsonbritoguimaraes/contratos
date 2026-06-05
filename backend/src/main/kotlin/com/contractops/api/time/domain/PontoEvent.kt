package com.contractops.api.time.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "ponto_events")
class PontoEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "contract_id")
    val contractId: UUID? = null,

    @Column(name = "competencia", nullable = false)
    val competencia: LocalDate,

    @Column(name = "codigo_rubrica", nullable = false, length = 50)
    val codigoRubrica: String,

    @Column(name = "descricao", length = 255)
    val descricao: String? = null,

    @Column(name = "tipo", nullable = false, length = 20)
    val tipo: String,

    @Column(name = "quantidade", precision = 10, scale = 2)
    val quantidade: BigDecimal? = null,

    @Column(name = "valor_unitario", precision = 14, scale = 2)
    val valorUnitario: BigDecimal? = null,

    @Column(name = "valor_total", precision = 14, scale = 2)
    val valorTotal: BigDecimal? = null,

    @Column(name = "origem", length = 30)
    val origem: String = "APURACAO",

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
