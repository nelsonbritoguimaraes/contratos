package com.contractops.api.time.service

import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.fiscal.crypto.CadesSignatureService
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.repository.BancoHorasRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * AEJ — Anexo VI Portaria 671/2021 (registros 01, 02, 03, 04, 05, 07, 99).
 */
@Service
class AejExportService(
    private val attendanceDayRepository: AttendanceDayRepository,
    private val servicePostRepository: ServicePostRepository,
    private val employeeRepository: EmployeeRepository,
    private val bancoHorasRepository: BancoHorasRepository,
    private val cadesSignatureService: CadesSignatureService
) {

    private val dhFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:00-0300")

    fun generateAej(contractId: UUID, period: LocalDate, tenantId: UUID): String {
        val body = buildAejBody(contractId, period, tenantId)
        val sig = signAej(body, contractId, period)
        return "$body\r\n#P7S_MODE=${sig.mode}\r\n#P7S_FILE=${sig.filename}\r\n#P7S=${sig.signatureBase64}"
    }

    fun generateAejP7s(contractId: UUID, period: LocalDate, tenantId: UUID): CadesSignatureService.CadesResult {
        val body = buildAejBody(contractId, period, tenantId)
        return signAej(body, contractId, period)
    }

    private fun buildAejBody(contractId: UUID, period: LocalDate, tenantId: UUID): String {
        val ym = YearMonth.from(period)
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()

        val attendances = attendanceDayRepository.findByTenantIdAndContractIdAndDateBetween(
            tenantId, contractId, start, end
        )
        val posts = servicePostRepository.findByContractId(contractId)

        val lines = mutableListOf<String>()
        // Reg 01 — Cabeçalho
        lines.add("01|1|2|${tenantId}|${contractId}|${start}|${end}|ContractOps PTRP|2.0|")

        // Reg 02 — REP
        lines.add("02|1|3|ContractOps REP-P|00000000000000|")

        val byEmployee = attendances.groupBy { it.employeeId }
        var seqVinculo = 1
        byEmployee.forEach { (employeeId, days) ->
            val emp = employeeRepository.findById(employeeId).orElse(null)
            val cpf = emp?.cpf?.replace(Regex("\\D"), "")?.take(11) ?: employeeId.toString().replace("-", "").take(11)
            // Reg 03 — Vínculo
            lines.add("03|$seqVinculo|$cpf|${emp?.fullName ?: "Colaborador"}|")

            // Reg 04 — Horário contratual (escala do posto)
            val post = days.firstNotNullOfOrNull { d -> d.postId }?.let { servicePostRepository.findById(it).orElse(null) }
            val codHor = post?.escala?.take(30) ?: "DIURNA_08_17"
            lines.add("04|$seqVinculo|$codHor|08:00|17:00|")

            var seqPar = 1
            days.sortedBy { it.date }.forEach { day ->
                day.firstEntry?.let { entrada ->
                    lines.add("05|$seqVinculo|${entrada.format(dhFormatter)}|1|E|$seqPar|O|$codHor|")
                }
                day.lastExit?.let { saida ->
                    lines.add("05|$seqVinculo|${saida.format(dhFormatter)}|1|S|$seqPar|S|O||")
                    seqPar++
                }
                if (day.absenceMinutes > 0 && day.totalWorkedMinutes == 0) {
                    lines.add("07|$seqVinculo|${day.date}|FALTA|${day.absenceMinutes}|")
                }
            }

            val bh = bancoHorasRepository.findByTenantIdAndEmployeeIdAndCompetencia(tenantId, employeeId, start)
            bh?.let {
                if (it.saldoMinutos != 0) {
                    lines.add("07|$seqVinculo|${end}|BANCO_HORAS|${it.saldoMinutos}|")
                }
            }
            seqVinculo++
        }

        lines.add("99|ContractOps|AEJ|posts=${posts.size}|attendance_days=${attendances.size}|")
        return lines.joinToString("\r\n")
    }

    fun signAej(content: String, contractId: UUID, period: LocalDate): CadesSignatureService.CadesResult {
        val base = "aej_${contractId}_${period.year}_${period.monthValue}"
        return cadesSignatureService.signDetached(content, base)
    }
}
