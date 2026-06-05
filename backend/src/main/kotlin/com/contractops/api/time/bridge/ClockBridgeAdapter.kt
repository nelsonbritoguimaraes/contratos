package com.contractops.api.time.bridge

import com.contractops.api.time.domain.RawPunch
import java.util.*

interface ClockBridgeAdapter {

    fun getVendor(): ClockVendor

    fun importPunches(
        content: String,
        deviceId: UUID?,
        tenantId: UUID
    ): ImportResult

    /**
     * Busca marcações diretamente do dispositivo via HTTP/TCP (stub).
     * Retorna null se o adaptador não suporta fetch remoto.
     */
    fun fetchFromDevice(
        apiUrl: String,
        deviceId: UUID?,
        tenantId: UUID
    ): ImportResult? = null

    data class ImportResult(
        val punches: List<RawPunch>,
        val errors: List<String>,
        val totalLines: Int,
        val vendor: ClockVendor
    )
}
