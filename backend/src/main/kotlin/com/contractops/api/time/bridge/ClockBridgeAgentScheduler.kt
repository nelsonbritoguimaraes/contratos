package com.contractops.api.time.bridge

import com.contractops.api.time.domain.ClockSyncStatus
import com.contractops.api.time.domain.TimeClockDevice
import com.contractops.api.time.repository.ClockSyncStatusRepository
import com.contractops.api.time.repository.TimeClockDeviceRepository
import com.contractops.api.time.service.TimePunchPipelineService
import com.contractops.api.time.service.TimePunchService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Component
class ClockBridgeAgentScheduler(
    private val deviceRepository: TimeClockDeviceRepository,
    private val clockBridgeService: ClockBridgeService,
    private val pipelineService: TimePunchPipelineService,
    private val timePunchService: TimePunchService,
    private val clockSyncStatusRepository: ClockSyncStatusRepository,
    private val adapters: List<ClockBridgeAdapter>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${contractops.clock-bridge.poll-ms:300000}", initialDelay = 60_000)
    @Transactional
    fun pollDevices() {
        val devices = deviceRepository.findAll().filter {
            it.status.uppercase() in listOf("ACTIVE", "ATIVO") && !it.apiUrl.isNullOrBlank()
        }
        if (devices.isEmpty()) return

        log.info("Clock Bridge Agent: {} dispositivos ativos para sync", devices.size)
        devices.forEach { device -> syncDevice(device) }
    }

    fun syncDevice(device: TimeClockDevice) {
        val syncStatus = clockSyncStatusRepository.findByTenantIdAndDeviceId(device.tenantId, device.id!!)
            ?: ClockSyncStatus(tenantId = device.tenantId, deviceId = device.id!!)

        try {
            val vendor = ClockVendor.fromName(device.manufacturer ?: device.model)
            val adapter = adapters.firstOrNull { it.getVendor() == vendor }
                ?: adapters.firstOrNull { it.getVendor() == ClockVendor.GENERIC_AFD }

            val result = adapter?.fetchFromDevice(device.apiUrl!!, device.id, device.tenantId)
                ?: ClockBridgeAdapter.ImportResult(emptyList(), listOf("Adaptador sem fetch remoto"), 0, vendor)

            val imported = if (result.punches.isNotEmpty()) {
                timePunchService.importRawPunches(result.punches, device.tenantId)
            } else 0

            if (imported > 0) {
                pipelineService.processImportedPunches(device.tenantId, device.contractId, result.punches)
            }

            device.lastSyncAt = OffsetDateTime.now()
            deviceRepository.save(device)

            syncStatus.lastSyncAt = OffsetDateTime.now()
            syncStatus.lastSyncStatus = if (result.errors.isEmpty()) "SUCCESS" else "PARTIAL"
            syncStatus.punchesImported = imported
            syncStatus.errorsCount = result.errors.size
            syncStatus.errorMessage = result.errors.take(3).joinToString("; ").ifBlank { null }
            syncStatus.nextSyncAt = OffsetDateTime.now().plusMinutes(5)
            clockSyncStatusRepository.save(syncStatus)

            log.info(
                "Sync device {} — vendor {} — imported {} punches (errors={})",
                device.name, result.vendor.name, imported, result.errors.size
            )
        } catch (ex: Exception) {
            syncStatus.lastSyncAt = OffsetDateTime.now()
            syncStatus.lastSyncStatus = "FAILED"
            syncStatus.errorsCount = syncStatus.errorsCount + 1
            syncStatus.errorMessage = ex.message
            syncStatus.nextSyncAt = OffsetDateTime.now().plusMinutes(15)
            clockSyncStatusRepository.save(syncStatus)
            log.warn("Falha sync device {}: {}", device.name, ex.message)
        }
    }

    fun getSyncStatuses(tenantId: UUID): List<ClockSyncStatus> =
        clockSyncStatusRepository.findByTenantId(tenantId)
}
