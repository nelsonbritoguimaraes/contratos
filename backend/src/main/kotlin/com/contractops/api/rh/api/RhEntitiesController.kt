package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.domain.EmployeeBenefit
import com.contractops.api.rh.domain.EmployeeVacation
import com.contractops.api.rh.domain.FloatWorker
import com.contractops.api.rh.domain.PayrollPeriod
import com.contractops.api.rh.repository.EmployeeBenefitRepository
import com.contractops.api.rh.repository.EmployeeVacationRepository
import com.contractops.api.rh.repository.FloatWorkerRepository
import com.contractops.api.rh.repository.PayrollPeriodRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/rh")
class RhEntitiesController(
    private val payrollPeriodRepository: PayrollPeriodRepository,
    private val vacationRepository: EmployeeVacationRepository,
    private val benefitRepository: EmployeeBenefitRepository,
    private val floatWorkerRepository: FloatWorkerRepository
) {

    @GetMapping("/payroll-periods")
    fun listPayrollPeriods(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<PayrollPeriod>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(payrollPeriodRepository.findByTenantIdOrderByCompetenceDesc(t))
    }

    @PostMapping("/payroll-periods")
    fun createPayrollPeriod(
        @RequestBody req: CreatePayrollPeriodRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<PayrollPeriod> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val saved = payrollPeriodRepository.save(
            PayrollPeriod(
                tenantId = t,
                contractId = req.contractId,
                competence = LocalDate.parse(req.competence).withDayOfMonth(1),
                status = req.status ?: "OPEN",
                notes = req.notes
            )
        )
        return ResponseEntity.created(URI.create("/api/rh/payroll-periods/${saved.id}")).body(saved)
    }

    @GetMapping("/vacations")
    fun listVacations(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<EmployeeVacation>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(vacationRepository.findByTenantIdOrderByStartDateDesc(t))
    }

    @PostMapping("/vacations")
    fun createVacation(
        @RequestBody req: CreateVacationRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeVacation> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val saved = vacationRepository.save(
            EmployeeVacation(
                tenantId = t,
                employeeId = req.employeeId,
                contractId = req.contractId,
                startDate = LocalDate.parse(req.startDate),
                endDate = LocalDate.parse(req.endDate),
                daysCount = req.daysCount,
                status = req.status ?: "PLANNED",
                notes = req.notes
            )
        )
        return ResponseEntity.created(URI.create("/api/rh/vacations/${saved.id}")).body(saved)
    }

    @GetMapping("/benefits")
    fun listBenefits(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<EmployeeBenefit>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(benefitRepository.findByTenantIdAndIsActiveTrue(t))
    }

    @PostMapping("/benefits")
    fun createBenefit(
        @RequestBody req: CreateBenefitRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeBenefit> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val saved = benefitRepository.save(
            EmployeeBenefit(
                tenantId = t,
                employeeId = req.employeeId,
                benefitType = req.benefitType,
                description = req.description,
                monthlyValue = req.monthlyValue ?: BigDecimal.ZERO,
                isActive = req.isActive ?: true
            )
        )
        return ResponseEntity.created(URI.create("/api/rh/benefits/${saved.id}")).body(saved)
    }

    @GetMapping("/float-workers")
    fun listFloatWorkers(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<FloatWorker>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(floatWorkerRepository.findByTenantIdOrderByCreatedAtDesc(t))
    }

    @PostMapping("/float-workers")
    fun createFloatWorker(
        @RequestBody req: CreateFloatWorkerRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<FloatWorker> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val saved = floatWorkerRepository.save(
            FloatWorker(
                tenantId = t,
                employeeId = req.employeeId,
                region = req.region,
                enabledContracts = req.enabledContracts,
                enabledFunctions = req.enabledFunctions,
                availabilityNotes = req.availabilityNotes,
                status = req.status ?: "DISPONIVEL",
                slaMinutes = req.slaMinutes ?: 60
            )
        )
        return ResponseEntity.created(URI.create("/api/rh/float-workers/${saved.id}")).body(saved)
    }
}

data class CreatePayrollPeriodRequest(
    val contractId: UUID? = null,
    val competence: String,
    val status: String? = null,
    val notes: String? = null
)

data class CreateVacationRequest(
    val employeeId: UUID,
    val contractId: UUID? = null,
    val startDate: String,
    val endDate: String,
    val daysCount: Int,
    val status: String? = null,
    val notes: String? = null
)

data class CreateBenefitRequest(
    val employeeId: UUID,
    val benefitType: String,
    val description: String? = null,
    val monthlyValue: BigDecimal? = null,
    val isActive: Boolean? = true
)

data class CreateFloatWorkerRequest(
    val employeeId: UUID,
    val region: String? = null,
    val enabledContracts: String? = null,
    val enabledFunctions: String? = null,
    val availabilityNotes: String? = null,
    val status: String? = null,
    val slaMinutes: Int? = null
)
