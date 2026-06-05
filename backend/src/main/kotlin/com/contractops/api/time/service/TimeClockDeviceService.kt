package com.contractops.api.time.service

import com.contractops.api.time.api.CreateTimeClockDeviceRequest
import com.contractops.api.time.api.UpdateTimeClockDeviceRequest
import com.contractops.api.time.domain.TimeClockDevice
import com.contractops.api.time.repository.TimeClockDeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TimeClockDeviceService(
    private val repository: TimeClockDeviceRepository
) {

    fun findAllByTenant(tenantId: UUID): List<TimeClockDevice> =
        repository.findByTenantId(tenantId)

    fun findById(id: UUID, tenantId: UUID): TimeClockDevice? =
        repository.findById(id).filter { it.tenantId == tenantId }.orElse(null)

    @Transactional
    fun create(tenantId: UUID, request: CreateTimeClockDeviceRequest): TimeClockDevice {
        val device = TimeClockDevice(
            tenantId = tenantId,
            contractId = request.contractId,
            name = request.name,
            manufacturer = request.manufacturer,
            model = request.model,
            serialNumber = request.serialNumber,
            deviceType = request.deviceType,
            ipAddress = request.ipAddress,
            apiUrl = request.apiUrl,
            status = request.status
        )
        return repository.save(device)
    }

    @Transactional
    fun update(id: UUID, tenantId: UUID, request: UpdateTimeClockDeviceRequest): TimeClockDevice? {
        val existing = findById(id, tenantId) ?: return null

        request.name?.let { existing.name = it }
        request.manufacturer?.let { existing.manufacturer = it }
        request.model?.let { existing.model = it }
        request.serialNumber?.let { existing.serialNumber = it }
        request.deviceType?.let { existing.deviceType = it }
        request.ipAddress?.let { existing.ipAddress = it }
        request.apiUrl?.let { existing.apiUrl = it }
        request.status?.let { existing.status = it }

        return repository.save(existing)
    }

    @Transactional
    fun delete(id: UUID, tenantId: UUID): Boolean {
        val existing = findById(id, tenantId) ?: return false
        repository.delete(existing)
        return true
    }
}