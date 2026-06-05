package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.domain.EmployeeEvent
import com.contractops.api.rh.service.EmployeeEventService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/rh/employees")
class EmployeeEventController(
    private val eventService: EmployeeEventService
) {

    @GetMapping("/{employeeId}/events")
    fun listEvents(
        @PathVariable employeeId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<EmployeeEventResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val events = eventService.findByEmployee(employeeId, effectiveTenant)
        return ResponseEntity.ok(events.map { EmployeeEventResponse.fromEntity(it) })
    }

    @PostMapping("/{employeeId}/events")
    fun registerEvent(
        @PathVariable employeeId: UUID,
        @Valid @RequestBody request: CreateEmployeeEventRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeEventResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val event = EmployeeEvent(
            tenantId = effectiveTenant,
            employeeId = employeeId,
            contractId = request.contractId,
            eventType = request.eventType,
            eventDate = request.eventDate,
            description = request.description,
            previousValue = request.previousValue,
            newValue = request.newValue,
            reason = request.reason,
            documentReference = request.documentReference,
            affectsPayrollFrom = request.affectsPayrollFrom
        )

        val created = eventService.registerEvent(event)

        return ResponseEntity
            .created(URI.create("/api/rh/employees/$employeeId/events/${created.id}"))
            .body(EmployeeEventResponse.fromEntity(created))
    }
}