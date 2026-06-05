package com.contractops.api.employee.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

/**
 * Alocação de Colaborador em Posto/Contrato.
 * Representa a ligação entre Employee ↔ ServicePost/Contract.
 * Essencial para cobertura, ponto e glosas.
 */
@Entity
@Table(name = "employee_assignments")
class EmployeeAssignment(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "post_id")
    val postId: UUID? = null,   // Pode ser alocado diretamente no contrato ou em um posto específico

    @Column(name = "role", length = 50)
    var role: String? = null,   // TITULAR, RESERVA, VOLANTE, SUPERVISOR

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

) : AuditEntity(), TenantAware {

    override fun toString(): String = "EmployeeAssignment(id=$id, employeeId=$employeeId, postId=$postId, role='$role')"
}
