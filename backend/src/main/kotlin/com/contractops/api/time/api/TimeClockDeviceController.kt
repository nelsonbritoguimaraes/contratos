package com.contractops.api.time.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.time.service.TimeClockDeviceService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * CRUD completo para dispositivos de ponto (relógios REP e sistemas alternativos).
 * Base para Clock Bridge Agent e integrações (Control iD, Topdata, etc.).
 * SPEC §9 e §10.
 */
@RestController
@RequestMapping("/api/time/clock-devices")
class TimeClockDeviceController(
    private val service: TimeClockDeviceService
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<TimeClockDeviceResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val devices = service.findAllByTenant(effectiveTenant)
        return ResponseEntity.ok(devices.map { TimeClockDeviceResponse.fromEntity(it) })
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<TimeClockDeviceResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val device = service.findById(id, effectiveTenant)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(TimeClockDeviceResponse.fromEntity(device))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateTimeClockDeviceRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<TimeClockDeviceResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val created = service.create(effectiveTenant, request)
        return ResponseEntity
            .created(URI.create("/api/time/clock-devices/${created.id}"))
            .body(TimeClockDeviceResponse.fromEntity(created))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTimeClockDeviceRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<TimeClockDeviceResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val updated = service.update(id, effectiveTenant, request)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(TimeClockDeviceResponse.fromEntity(updated))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Void> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val deleted = service.delete(id, effectiveTenant)
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }
}