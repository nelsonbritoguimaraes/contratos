package com.contractops.api.time.service

import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.domain.AttendanceDay
import com.contractops.api.time.domain.NormalizedPunch
import com.contractops.api.time.domain.RawPunch
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.repository.NormalizedPunchRepository
import com.contractops.api.time.repository.RawPunchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class AttendanceProcessingService(
    private val rawPunchRepository: RawPunchRepository,
    private val normalizedPunchRepository: NormalizedPunchRepository,
    private val attendanceDayRepository: AttendanceDayRepository,
    private val employeeRepository: EmployeeRepository,
    private val servicePostRepository: ServicePostRepository,
    private val contractRepository: ContractRepository,
    private val assignmentRepository: EmployeeAssignmentRepository,
    private val employeePunchResolver: EmployeePunchResolver,
    private val regrasPontoParser: RegrasPontoParser,
    private val coverageService: CoverageService
) {

    @Transactional
    fun processEmployeeDay(
        employeeId: UUID,
        date: LocalDate,
        tenantId: UUID,
        postId: UUID? = null,
        contractId: UUID? = null
    ): AttendanceDay? {
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return null

        val assignment = employeePunchResolver.activeAssignment(tenantId, employeeId)
        val effectiveContractId = contractId ?: assignment?.contractId
        val effectivePostId = postId ?: assignment?.postId

        val regras = effectiveContractId?.let { cid ->
            contractRepository.findById(cid).orElse(null)?.regrasPonto?.let { regrasPontoParser.parse(it) }
        } ?: RegrasPontoParser.RegrasPonto()

        val startOfDay = date.atStartOfDay()
        val endOfDay = date.plusDays(1).atStartOfDay()

        val rawPunches = rawPunchRepository.findByTenantIdAndPunchTimestampBetween(tenantId, startOfDay, endOfDay)
            .filter { raw ->
                raw.employeeId == employeeId ||
                    (raw.employeeId == null && (
                        raw.matricula?.let { it == employee.matricula } == true ||
                            employeePunchResolver.normalizeCpf(raw.cpf) == employeePunchResolver.normalizeCpf(employee.cpf)
                        ))
            }

        normalizedPunchRepository.deleteByTenantIdAndEmployeeIdAndPunchTimestampBetween(
            tenantId, employeeId, startOfDay, endOfDay
        )

        if (rawPunches.isEmpty()) {
            return upsertAttendanceDay(
                employeeId, date, tenantId, emptyList(), emptyList(),
                effectivePostId, effectiveContractId, regras
            )
        }

        val normalized = rawPunches.map { raw ->
            NormalizedPunch(
                tenantId = tenantId,
                rawPunchId = raw.id,
                employeeId = employeeId,
                postId = effectivePostId,
                contractId = effectiveContractId,
                punchTimestamp = raw.punchTimestamp,
                punchType = raw.punchType ?: "ENTRADA",
                source = raw.sourceChannel ?: "DEVICE"
            )
        }.map { normalizedPunchRepository.save(it) }

        return upsertAttendanceDay(
            employeeId, date, tenantId, rawPunches, normalized,
            effectivePostId, effectiveContractId, regras
        )
    }

    private fun upsertAttendanceDay(
        employeeId: UUID,
        date: LocalDate,
        tenantId: UUID,
        rawPunches: List<RawPunch>,
        normalized: List<NormalizedPunch>,
        postId: UUID?,
        contractId: UUID?,
        regras: RegrasPontoParser.RegrasPonto
    ): AttendanceDay {
        val sorted = normalized.sortedBy { it.punchTimestamp }

        var firstEntry: LocalDateTime? = null
        var lastExit: LocalDateTime? = null
        var totalWorkedMinutes = 0L

        for (i in sorted.indices step 2) {
            val entry = sorted.getOrNull(i)
            val exit = sorted.getOrNull(i + 1)
            if (entry != null && firstEntry == null) firstEntry = entry.punchTimestamp
            if (exit != null) lastExit = exit.punchTimestamp
            if (entry != null && exit != null) {
                totalWorkedMinutes += Duration.between(entry.punchTimestamp, exit.punchTimestamp).toMinutes()
            }
        }

        val rawDelay = calculateDelayMinutes(date, firstEntry, postId)
        val delayMinutes = regrasPontoParser.applyTolerancia(rawDelay, regras)
        val jornada = regras.jornadaDiariaMinutos
        val absenceMinutes = when {
            normalized.isEmpty() -> jornada
            totalWorkedMinutes < jornada / 2 -> jornada - totalWorkedMinutes.toInt()
            else -> 0
        }

        val existing = attendanceDayRepository.findByTenantIdAndEmployeeIdAndDate(tenantId, employeeId, date)
        val attendance = existing ?: AttendanceDay(
            tenantId = tenantId,
            employeeId = employeeId,
            date = date
        )

        attendance.postId = postId
        attendance.contractId = contractId
        attendance.firstEntry = firstEntry
        attendance.lastExit = lastExit
        attendance.totalWorkedMinutes = totalWorkedMinutes.toInt()
        attendance.delayMinutes = delayMinutes
        attendance.absenceMinutes = absenceMinutes.coerceAtLeast(0)
        attendance.source = "AUTO_PROCESSED"

        return attendanceDayRepository.save(attendance)
    }

    internal fun calculateDelayMinutes(date: LocalDate, firstEntry: LocalDateTime?, postId: UUID?): Int {
        if (firstEntry == null || postId == null) return 0
        val post = servicePostRepository.findById(postId).orElse(null) ?: return 0
        val expected = EscalaScheduleParser.expectedEntryTime(date, post.escala) ?: return 0
        return EscalaScheduleParser.calculateDelayMinutes(firstEntry, expected)
    }

    fun getDailyCoverageSummary(contractId: UUID, date: LocalDate, tenantId: UUID): Map<String, Any> =
        coverageService.getDailyCoverageSummary(contractId, date, tenantId)

    fun getMonthlyEmployeeSummary(
        employeeId: UUID,
        contractId: UUID,
        competencia: LocalDate,
        tenantId: UUID
    ): Map<String, Any> {
        val start = competencia.withDayOfMonth(1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        val days = attendanceDayRepository.findByTenantIdAndEmployeeIdAndDateBetween(tenantId, employeeId, start, end)
            .filter { it.contractId == null || it.contractId == contractId }

        val totalWorked = days.sumOf { it.totalWorkedMinutes }
        val totalAbsence = days.sumOf { it.absenceMinutes }
        val totalDelay = days.sumOf { it.delayMinutes }
        val faltaDias = days.count { it.absenceMinutes >= 240 && it.totalWorkedMinutes == 0 }

        val contract = contractRepository.findById(contractId).orElse(null)
        val regras = regrasPontoParser.parse(contract?.regrasPonto)
        val jornadaMensal = regras.jornadaDiariaMinutos * 22
        val heMinutos = (totalWorked - jornadaMensal).coerceAtLeast(0)

        return mapOf(
            "employeeId" to employeeId,
            "contractId" to contractId,
            "competencia" to start.toString(),
            "totalWorkedMinutes" to totalWorked,
            "totalAbsenceMinutes" to totalAbsence,
            "totalDelayMinutes" to totalDelay,
            "faltaDias" to faltaDias,
            "horasExtrasMinutos" to heMinutos,
            "diasApurados" to days.size
        )
    }

    fun detectVolantes(tenantId: UUID, contractId: UUID, date: LocalDate): List<Map<String, Any>> {
        val assignments = assignmentRepository.findByTenantIdAndContractId(tenantId, contractId)
            .filter { it.isActive && it.role?.uppercase() == "VOLANTE" }
        val attendances = attendanceDayRepository.findByTenantIdAndContractIdAndDate(tenantId, contractId, date)
        val present = attendances.filter { it.totalWorkedMinutes > 0 }.map { it.employeeId }.toSet()

        return assignments.filter { it.employeeId !in present }.map { a ->
            mapOf<String, Any>(
                "employeeId" to a.employeeId,
                "postId" to (a.postId as Any? ?: ""),
                "role" to (a.role ?: ""),
                "status" to "AUSENTE"
            )
        }
    }

    fun gerarEspelhoPonto(employeeId: UUID, competencia: LocalDate, tenantId: UUID): Map<String, Any> {
        val inicio = competencia.withDayOfMonth(1)
        val fim = inicio.withDayOfMonth(inicio.lengthOfMonth())
        val dias = attendanceDayRepository.findByTenantIdAndEmployeeIdAndDateBetween(tenantId, employeeId, inicio, fim)
        val emp = employeeRepository.findById(employeeId).orElse(null)
        val linhas = dias.sortedBy { it.date }.map { d ->
            mapOf(
                "data" to d.date.toString(),
                "entrada" to d.firstEntry?.toString(),
                "saida" to d.lastExit?.toString(),
                "minutosTrabalhados" to d.totalWorkedMinutes,
                "atrasoMinutos" to d.delayMinutes,
                "faltaMinutos" to d.absenceMinutes,
                "contratoId" to d.contractId,
                "postoId" to d.postId
            )
        }
        return mapOf<String, Any>(
            "employeeId" to employeeId,
            "nome" to (emp?.fullName ?: ""),
            "cpf" to (emp?.cpf ?: ""),
            "matricula" to (emp?.matricula ?: ""),
            "competencia" to inicio.toString(),
            "totalMinutos" to dias.sumOf { it.totalWorkedMinutes },
            "diasComMarcacao" to dias.count { it.totalWorkedMinutes > 0 },
            "linhas" to linhas,
            "titulo" to "ESPELHO DE PONTO ELETRÔNICO — Portaria 671/2021"
        )
    }
}
