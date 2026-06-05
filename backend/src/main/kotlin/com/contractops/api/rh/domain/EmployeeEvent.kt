package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Evento de Departamento Pessoal (DP) do colaborador.
 * Registra todas as movimentações que impactam a folha de pagamento.
 *
 * Alinhado com SPEC §8.2 Eventos de DP.
 */
@Entity
@Table(name = "employee_events")
class EmployeeEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "contract_id")
    val contractId: UUID? = null,

    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: String,   // ADMISSION, SALARY_CHANGE, PROMOTION, VACATION_START, VACATION_END, TERMINATION, etc.

    @Column(name = "event_date", nullable = false)
    var eventDate: LocalDate,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    // Campos específicos por tipo de evento (armazenados em JSON simples ou colunas)
    @Column(name = "previous_value", precision = 14, scale = 2)
    var previousValue: BigDecimal? = null,

    @Column(name = "new_value", precision = 14, scale = 2)
    var newValue: BigDecimal? = null,

    @Column(name = "reason", length = 200)
    var reason: String? = null,

    @Column(name = "document_reference", length = 100)
    var documentReference: String? = null,

    @Column(name = "affects_payroll_from")
    var affectsPayrollFrom: LocalDate? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "EmployeeEvent(id=$id, employeeId=$employeeId, type='$eventType', date=$eventDate)"
}