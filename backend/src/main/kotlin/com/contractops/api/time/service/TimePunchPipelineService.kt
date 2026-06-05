package com.contractops.api.time.service

import com.contractops.api.time.domain.AttendanceDay
import com.contractops.api.time.domain.RawPunch
import com.contractops.api.time.repository.RawPunchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.*

@Service
class TimePunchPipelineService(
    private val afdImportService: AfdImportService,
    private val timePunchService: TimePunchService,
    private val attendanceProcessingService: AttendanceProcessingService,
    private val rawPunchRepository: RawPunchRepository,
    private val employeePunchResolver: EmployeePunchResolver
) {

    data class ImportPipelineResult(
        val imported: Int,
        val totalParsed: Int,
        val formato: String,
        val errors: List<String>,
        val processedDays: Int,
        val employeesProcessed: Int
    )

    @Transactional
    fun importAfdAndProcess(
        file: MultipartFile,
        tenantId: UUID,
        deviceId: UUID?,
        contractId: UUID?,
        autoProcess: Boolean = true
    ): ImportPipelineResult {
        val content = file.inputStream.bufferedReader().use { it.readText() }
        val parsed = afdImportService.parseAfdWithReport(content, deviceId, tenantId)
        val imported = timePunchService.importRawPunches(parsed.punches, tenantId)

        var processedDays = 0
        var employeesProcessed = 0
        if (autoProcess && imported > 0) {
            val batch = processImportedPunches(tenantId, contractId, parsed.punches)
            processedDays = batch.processedDays
            employeesProcessed = batch.employeesProcessed
        }

        return ImportPipelineResult(
            imported = imported,
            totalParsed = parsed.punches.size,
            formato = parsed.formato,
            errors = parsed.errors,
            processedDays = processedDays,
            employeesProcessed = employeesProcessed
        )
    }

    @Transactional
    fun processImportedPunches(
        tenantId: UUID,
        contractId: UUID?,
        punches: List<RawPunch>
    ): BatchProcessResult {
        val groups = punches
            .mapNotNull { p ->
                val empId = p.employeeId ?: employeePunchResolver.resolve(tenantId, p.matricula, p.cpf)?.id
                empId?.let { it to p.punchTimestamp.toLocalDate() }
            }
            .distinct()

        var processedDays = 0
        groups.forEach { (employeeId, date) ->
            val assignment = employeePunchResolver.activeAssignment(tenantId, employeeId)
            val cid = contractId ?: assignment?.contractId
            val postId = assignment?.postId
            attendanceProcessingService.processEmployeeDay(employeeId, date, tenantId, postId, cid)?.let {
                processedDays++
            }
        }
        return BatchProcessResult(processedDays, groups.map { it.first }.distinct().size)
    }

    @Transactional
    fun processContractMonth(tenantId: UUID, contractId: UUID, competencia: LocalDate): BatchProcessResult {
        val start = competencia.withDayOfMonth(1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        val punches = rawPunchRepository.findByTenantIdAndPunchTimestampBetween(
            tenantId,
            start.atStartOfDay(),
            end.plusDays(1).atStartOfDay()
        )
        return processImportedPunches(tenantId, contractId, punches)
    }

    data class BatchProcessResult(val processedDays: Int, val employeesProcessed: Int)
}
