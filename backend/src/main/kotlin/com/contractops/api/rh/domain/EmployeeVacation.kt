package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "employee_vacations")
class EmployeeVacation(
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

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(name = "days_count", nullable = false)
    var daysCount: Int,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "PLANNED",

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
) : AuditEntity(), TenantAware
