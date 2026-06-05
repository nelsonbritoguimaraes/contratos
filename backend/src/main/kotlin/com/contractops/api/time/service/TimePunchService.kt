package com.contractops.api.time.service

import com.contractops.api.time.domain.RawPunch
import com.contractops.api.time.repository.RawPunchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class TimePunchService(
    private val rawPunchRepository: RawPunchRepository,
    private val employeePunchResolver: EmployeePunchResolver
) {

    fun findPunchesByPeriod(tenantId: UUID, start: LocalDateTime, end: LocalDateTime): List<RawPunch> =
        rawPunchRepository.findByTenantIdAndPunchTimestampBetween(tenantId, start, end)

    @Transactional
    fun importRawPunches(punches: List<RawPunch>, tenantId: UUID? = null): Int {
        return importRawPunchesDetailed(punches, tenantId).imported
    }

    @Transactional
    fun importRawPunchesDetailed(punches: List<RawPunch>, tenantId: UUID? = null): ImportResult {
        var imported = 0
        val saved = mutableListOf<RawPunch>()
        punches.forEach { punch ->
            val tid = tenantId ?: punch.tenantId
            val resolved = resolveEmployeeOnPunch(punch, tid)
            val exists = when {
                resolved.deviceId != null && resolved.nsr != null ->
                    rawPunchRepository.findByDeviceIdAndNsr(resolved.deviceId!!, resolved.nsr!!) != null
                else -> false
            }
            if (!exists) {
                saved.add(rawPunchRepository.save(resolved))
                imported++
            }
        }
        return ImportResult(imported, saved)
    }

    data class ImportResult(val imported: Int, val saved: List<RawPunch>)

    private fun resolveEmployeeOnPunch(punch: RawPunch, tenantId: UUID): RawPunch {
        if (punch.employeeId != null) return punch
        val emp = employeePunchResolver.resolve(tenantId, punch.matricula, punch.cpf) ?: return punch
        return RawPunch(
            tenantId = punch.tenantId,
            deviceId = punch.deviceId,
            employeeId = emp.id,
            matricula = punch.matricula ?: emp.matricula,
            cpf = punch.cpf ?: employeePunchResolver.normalizeCpf(emp.cpf),
            punchTimestamp = punch.punchTimestamp,
            punchType = punch.punchType,
            nsr = punch.nsr,
            rawData = punch.rawData,
            importBatchId = punch.importBatchId,
            latitude = punch.latitude,
            longitude = punch.longitude,
            sourceChannel = punch.sourceChannel
        )
    }
}
