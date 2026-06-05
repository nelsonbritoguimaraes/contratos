package com.contractops.api.time.service

import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.rh.domain.PayrollRubric
import com.contractops.api.rh.repository.PayrollRubricRepository
import com.contractops.api.time.domain.BancoHoras
import com.contractops.api.time.domain.PontoEvent
import com.contractops.api.time.repository.BancoHorasRepository
import com.contractops.api.time.repository.PontoEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

/**
 * Gera eventos de ponto → rubricas de folha (HORA_EXTRA, PONTO_FALTA, ATRASO, COBERTURA_PENALTY).
 */
@Service
class PontoPayrollIntegrationService(
    private val attendanceProcessingService: AttendanceProcessingService,
    private val pontoEventRepository: PontoEventRepository,
    private val bancoHorasRepository: BancoHorasRepository,
    private val rubricRepository: PayrollRubricRepository,
    private val employeeRepository: EmployeeRepository,
    private val contractRepository: ContractRepository,
    private val regrasPontoParser: RegrasPontoParser
) {

    @Transactional
    fun gerarEventosMensais(employeeId: UUID, contractId: UUID, competencia: LocalDate, tenantId: UUID): List<PontoEvent> {
        val comp = competencia.withDayOfMonth(1)
        pontoEventRepository.deleteByTenantIdAndEmployeeIdAndCompetencia(tenantId, employeeId, comp)

        val summary = attendanceProcessingService.getMonthlyEmployeeSummary(employeeId, contractId, comp, tenantId)
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return emptyList()
        val baseSalary = employee.salaryBase ?: BigDecimal.ZERO
        val contract = contractRepository.findById(contractId).orElse(null)
        val regras = regrasPontoParser.parse(contract?.regrasPonto)
        val valorHora = regrasPontoParser.valorHora(baseSalary)

        val events = mutableListOf<PontoEvent>()

        val heMinutos = summary["horasExtrasMinutos"] as Int
        if (heMinutos > 0) {
            val horas = BigDecimal(heMinutos).divide(BigDecimal("60"), 2, RoundingMode.HALF_UP)
            val fator = BigDecimal("1").add(BigDecimal(regras.hePercentual).divide(BigDecimal("100")))
            events.add(
                PontoEvent(
                    tenantId = tenantId,
                    employeeId = employeeId,
                    contractId = contractId,
                    competencia = comp,
                    codigoRubrica = "HORA_EXTRA",
                    descricao = "Horas extras apuradas",
                    tipo = "PROVENTO",
                    quantidade = horas,
                    valorUnitario = valorHora.multiply(fator),
                    valorTotal = valorHora.multiply(fator).multiply(horas)
                )
            )
        }

        val faltaDias = summary["faltaDias"] as Int
        if (faltaDias > 0) {
            val valorDia = baseSalary.divide(BigDecimal("30"), 2, RoundingMode.HALF_UP)
            events.add(
                PontoEvent(
                    tenantId = tenantId,
                    employeeId = employeeId,
                    contractId = contractId,
                    competencia = comp,
                    codigoRubrica = "PONTO_FALTA",
                    descricao = "Faltas apuradas ($faltaDias dias)",
                    tipo = "DESCONTO",
                    quantidade = BigDecimal(faltaDias),
                    valorUnitario = valorDia,
                    valorTotal = valorDia.multiply(BigDecimal(faltaDias))
                )
            )
        }

        val delayMin = summary["totalDelayMinutes"] as Int
        if (delayMin > 0) {
            val horasAtraso = BigDecimal(delayMin).divide(BigDecimal("60"), 2, RoundingMode.HALF_UP)
            events.add(
                PontoEvent(
                    tenantId = tenantId,
                    employeeId = employeeId,
                    contractId = contractId,
                    competencia = comp,
                    codigoRubrica = "ATRASO",
                    descricao = "Atrasos apurados",
                    tipo = "DESCONTO",
                    quantidade = horasAtraso,
                    valorUnitario = valorHora,
                    valorTotal = valorHora.multiply(horasAtraso)
                )
            )
        }

        val coverage = attendanceProcessingService.getDailyCoverageSummary(contractId, comp, tenantId)
        val coveragePct = coverage["coverage_percent"] as Double
        if (coveragePct < regras.coberturaMeta) {
            val penaltyPoints = ((regras.coberturaMeta - coveragePct) / 5.0).toInt().coerceAtLeast(1)
            val penalty = baseSalary.multiply(BigDecimal("0.01")).multiply(BigDecimal(penaltyPoints))
            events.add(
                PontoEvent(
                    tenantId = tenantId,
                    employeeId = employeeId,
                    contractId = contractId,
                    competencia = comp,
                    codigoRubrica = "COBERTURA_PENALTY",
                    descricao = "Penalidade cobertura ${coveragePct.toInt()}%",
                    tipo = "DESCONTO",
                    quantidade = BigDecimal.ONE,
                    valorTotal = penalty
                )
            )
        }

        if (regras.bancoHorasAtivo && heMinutos > 0) {
            updateBancoHoras(tenantId, employeeId, contractId, comp, heMinutos)
        }

        return events.map { pontoEventRepository.save(it) }
    }

    private fun updateBancoHoras(
        tenantId: UUID,
        employeeId: UUID,
        contractId: UUID,
        competencia: LocalDate,
        creditoMinutos: Int
    ) {
        val bh = bancoHorasRepository.findByTenantIdAndEmployeeIdAndCompetencia(tenantId, employeeId, competencia)
            ?: BancoHoras(tenantId = tenantId, employeeId = employeeId, contractId = contractId, competencia = competencia)
        bh.creditoMinutos += creditoMinutos
        bh.saldoMinutos = bh.creditoMinutos - bh.debitoMinutos
        bancoHorasRepository.save(bh)
    }

    fun findOrCreateRubric(tenantId: UUID, code: String): PayrollRubric? =
        rubricRepository.findByTenantIdAndIsActiveTrue(tenantId).firstOrNull { it.code == code }
            ?: rubricRepository.findByTenantIdAndIsActiveTrue(tenantId).firstOrNull { it.code.uppercase() == code.uppercase() }
}
