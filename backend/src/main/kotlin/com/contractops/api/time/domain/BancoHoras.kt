package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "banco_horas")
class BancoHoras(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    var employeeId: UUID,

    @Column(name = "contract_id")
    var contractId: UUID? = null,

    @Column(name = "competencia", nullable = false)
    var competencia: LocalDate,

    @Column(name = "saldo_minutos", nullable = false)
    var saldoMinutos: Int = 0,

    @Column(name = "credito_minutos", nullable = false)
    var creditoMinutos: Int = 0,

    @Column(name = "debito_minutos", nullable = false)
    var debitoMinutos: Int = 0,

    @Column(name = "observacao", columnDefinition = "TEXT")
    var observacao: String? = null
) : AuditEntity()
