package com.contractops.api.schedule.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.schedule.domain.EmployeeRoster
import com.contractops.api.schedule.domain.PostSchedule
import com.contractops.api.schedule.domain.ShiftTemplate
import com.contractops.api.schedule.service.ScheduleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/schedules")
class ScheduleController(private val service: ScheduleService) {

    @GetMapping("/templates")
    fun templates(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(required = false) contractId: UUID?
    ) = ResponseEntity.ok(service.listTemplates(tenant(tenantId), contractId))

    @PostMapping("/templates")
    fun createTemplate(@RequestBody req: CreateShiftTemplateRequest, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.createTemplate(ShiftTemplate(
            tenantId = tenant(tenantId), contractId = req.contractId, name = req.name,
            shiftType = req.shiftType, workHours = req.workHours, restHours = req.restHours,
            entryTime = req.entryTime, exitTime = req.exitTime, cycleDays = req.cycleDays
        )))

    @GetMapping("/posts")
    fun postSchedules(
        @RequestParam contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(service.listPostSchedules(tenant(tenantId), contractId))

    @PostMapping("/posts")
    fun createPostSchedule(@RequestBody req: CreatePostScheduleRequest, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.createPostSchedule(PostSchedule(
            tenantId = tenant(tenantId), contractId = req.contractId, postId = req.postId,
            shiftTemplateId = req.shiftTemplateId, effectiveFrom = req.effectiveFrom,
            effectiveTo = req.effectiveTo, scheduleType = req.scheduleType, notes = req.notes
        )))

    @GetMapping("/rosters")
    fun rosters(
        @RequestParam contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(service.listRosters(tenant(tenantId), contractId))

    @PostMapping("/rosters")
    fun createRoster(@RequestBody req: CreateEmployeeRosterRequest, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.createRoster(EmployeeRoster(
            tenantId = tenant(tenantId), contractId = req.contractId, postId = req.postId,
            employeeId = req.employeeId, postScheduleId = req.postScheduleId,
            effectiveFrom = req.effectiveFrom, effectiveTo = req.effectiveTo, role = req.role
        )))

    @GetMapping("/grid")
    fun scheduleGrid(
        @RequestParam contractId: UUID,
        @RequestParam start: String,
        @RequestParam end: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<Map<String, Any?>>> =
        ResponseEntity.ok(
            service.generateScheduleGrid(tenant(tenantId), contractId, LocalDate.parse(start), LocalDate.parse(end))
        )

    private fun tenant(tenantId: UUID?) =
        tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
}
