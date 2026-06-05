package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "employee_compliance")
class EmployeeCompliance(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "tipo", nullable = false, length = 30)
    var tipo: String,

    @Column(name = "descricao", length = 255)
    var descricao: String? = null,

    @Column(name = "data_realizacao")
    var dataRealizacao: LocalDate? = null,

    @Column(name = "data_validade")
    var dataValidade: LocalDate? = null,

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "VALIDO",

    @Column(name = "documento_ref", length = 100)
    var documentoRef: String? = null,

    @Column(name = "observacao", columnDefinition = "TEXT")
    var observacao: String? = null
) : AuditEntity(), TenantAware
