package com.contractops.api.time.service

import com.contractops.api.time.domain.RawPunch
import org.springframework.stereotype.Service
import java.util.*

/**
 * Fachada de importação AFD — delega ao parser oficial Anexo V + delimitado.
 */
@Service
class AfdImportService(
    private val officialParser: AfdOfficialParser
) {

    data class AfdParseResult(
        val punches: List<RawPunch>,
        val errors: List<String>,
        val totalLines: Int,
        val formato: String = "LEGADO",
        val crcValid: Boolean? = null
    )

    fun parseAfdContent(afdContent: String, deviceId: UUID?, tenantId: UUID): List<RawPunch> =
        parseAfdWithReport(afdContent, deviceId, tenantId).punches

    fun parseAfdWithReport(afdContent: String, deviceId: UUID?, tenantId: UUID): AfdParseResult {
        val result = officialParser.parse(afdContent, deviceId, tenantId)
        return AfdParseResult(
            punches = result.punches,
            errors = result.errors,
            totalLines = result.totalLines,
            formato = result.formato,
            crcValid = result.crcValid
        )
    }
}
