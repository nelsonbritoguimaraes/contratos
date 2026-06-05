package com.contractops.api.time.api

import java.time.OffsetDateTime
import java.util.*

data class TimeClockDeviceResponse(
    val id: UUID?,
    val contractId: UUID?,
    val name: String,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val deviceType: String?,
    val ipAddress: String?,
    val apiUrl: String?,
    val lastSyncAt: OffsetDateTime?,
    val status: String
) {
    companion object {
        fun fromEntity(d: com.contractops.api.time.domain.TimeClockDevice): TimeClockDeviceResponse =
            TimeClockDeviceResponse(
                id = d.id,
                contractId = d.contractId,
                name = d.name,
                manufacturer = d.manufacturer,
                model = d.model,
                serialNumber = d.serialNumber,
                deviceType = d.deviceType,
                ipAddress = d.ipAddress,
                apiUrl = d.apiUrl,
                lastSyncAt = d.lastSyncAt,
                status = d.status
            )
    }
}

data class CreateTimeClockDeviceRequest(
    val contractId: UUID? = null,
    val name: String,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val deviceType: String? = null,
    val ipAddress: String? = null,
    val apiUrl: String? = null,
    val status: String = "ACTIVE"
)

data class UpdateTimeClockDeviceRequest(
    val name: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val deviceType: String? = null,
    val ipAddress: String? = null,
    val apiUrl: String? = null,
    val status: String? = null
)