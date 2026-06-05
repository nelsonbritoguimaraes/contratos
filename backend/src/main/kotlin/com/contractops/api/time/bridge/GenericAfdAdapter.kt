package com.contractops.api.time.bridge

import com.contractops.api.time.service.AfdImportService
import org.springframework.stereotype.Component
import java.util.*

/**
 * Adaptador genérico que reutiliza o AfdImportService existente.
 * Serve como fallback e base para a maioria dos fabricantes que exportam AFD padrão.
 */
@Component
class GenericAfdAdapter(
    private val afdImportService: AfdImportService
) : ClockBridgeAdapter {

    override fun getVendor(): ClockVendor = ClockVendor.GENERIC_AFD

    override fun importPunches(
        content: String,
        deviceId: UUID?,
        tenantId: UUID
    ): ClockBridgeAdapter.ImportResult {
        val result = afdImportService.parseAfdWithReport(content, deviceId, tenantId)
        return ClockBridgeAdapter.ImportResult(
            punches = result.punches,
            errors = result.errors,
            totalLines = result.totalLines,
            vendor = getVendor()
        )
    }
}