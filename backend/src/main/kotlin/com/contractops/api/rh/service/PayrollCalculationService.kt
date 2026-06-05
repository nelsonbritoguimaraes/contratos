package com.contractops.api.rh.service

import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.rh.domain.PayrollRubric
import com.contractops.api.rh.domain.Payslip
import com.contractops.api.rh.domain.PayslipItem
import com.contractops.api.rh.repository.EmployeeEventRepository
import com.contractops.api.rh.repository.PayrollRubricRepository
import com.contractops.api.rh.repository.PayslipItemRepository
import com.contractops.api.rh.repository.PayslipRepository
import com.contractops.api.time.service.AttendanceProcessingService
import com.contractops.api.time.service.PontoPayrollIntegrationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Serviço responsável pelo cálculo da folha de pagamento (MVP aprimorado).
 *
 * Integra:
 * - Salário base real do Employee + histórico de alterações salariais via EmployeeEvent
 * - Attendance (cobertura, faltas)
 * - Rubricas ativas do tenant
 *
 * Regras mais avançadas (hora extra, adicional noturno, periculosidade, etc.) podem ser expandidas depois.
 */
@Service
class PayrollCalculationService(
    private val payslipRepository: PayslipRepository,
    private val payslipItemRepository: PayslipItemRepository,
    private val rubricRepository: PayrollRubricRepository,
    private val employeeRepository: EmployeeRepository,
    private val employeeEventRepository: EmployeeEventRepository,
    private val attendanceService: AttendanceProcessingService,
    private val pontoPayrollIntegration: PontoPayrollIntegrationService
) {

    @Transactional
    fun calculatePayslip(
        employeeId: UUID,
        contractId: UUID,
        competence: LocalDate,
        tenantId: UUID
    ): Payslip {

        // 1. Verificar se já existe holerite aprovado
        val existing = payslipRepository.findByTenantIdAndEmployeeIdAndCompetence(tenantId, employeeId, competence)
        if (existing != null && existing.status != "DRAFT") {
            throw IllegalStateException("Já existe holerite aprovado para esta competência")
        }

        // 2. Buscar Employee e salário atual
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        // 3. Buscar salário válido na competência (histórico elegante via EmployeeEvent)
        val salaryChangeEvents = employeeEventRepository
            .findByTenantIdAndEmployeeIdOrderByEventDateDesc(tenantId, employeeId)
            .filter { it.eventType == "SALARY_CHANGE" && it.eventDate <= competence }

        val baseSalary = if (salaryChangeEvents.isNotEmpty()) {
            salaryChangeEvents.first().newValue ?: employee.salaryBase ?: BigDecimal.ZERO
        } else {
            employee.salaryBase ?: BigDecimal.ZERO
        }

        if (baseSalary <= BigDecimal.ZERO) {
            throw IllegalStateException("Salário base do colaborador está zerado ou inválido")
        }

        // 4. Buscar rubricas ativas
        val activeRubrics = rubricRepository.findByTenantIdAndIsActiveTrue(tenantId)

        // 5. Apuração mensal de ponto → eventos/rubricas
        val comp = competence.withDayOfMonth(1)
        val pontoEvents = pontoPayrollIntegration.gerarEventosMensais(employeeId, contractId, comp, tenantId)
        val monthlySummary = attendanceService.getMonthlyEmployeeSummary(employeeId, contractId, comp, tenantId)
        val coverageSummary = attendanceService.getDailyCoverageSummary(contractId, comp, tenantId)
        val coveragePercent = (coverageSummary["coverage_percent"] as? Double) ?: 100.0
        val totalWorkedMinutes = monthlySummary["totalWorkedMinutes"] as Int

        // 6. Cálculo + Geração de Itens
        var totalEarnings = baseSalary
        var totalDeductions = BigDecimal.ZERO
        val itemsToSave = mutableListOf<PayslipItem>()

        // Processar rubricas e criar itens
        activeRubrics.sortedBy { it.displayOrder }.forEach { rubric ->
            var itemValue = BigDecimal.ZERO

            when (rubric.calculationType.uppercase()) {
                "FIXED" -> itemValue = rubric.fixedValue ?: BigDecimal.ZERO
                "PERCENTAGE_OF_BASE" -> {
                    val pct = rubric.percentage ?: BigDecimal.ZERO
                    itemValue = baseSalary.multiply(pct.divide(BigDecimal("100")))
                }
            }

            if (itemValue > BigDecimal.ZERO) {
                val item = PayslipItem(
                    tenantId = tenantId,
                    payslip = Payslip( // placeholder, será sobrescrito após salvar o payslip
                        tenantId = tenantId,
                        employeeId = employeeId,
                        contractId = contractId,
                        competence = competence
                    ),
                    rubric = rubric,
                    description = rubric.description,
                    quantity = BigDecimal.ONE,
                    unitValue = if (rubric.calculationType.uppercase() == "FIXED") itemValue else baseSalary,
                    totalValue = itemValue,
                    type = rubric.type
                )

                itemsToSave.add(item)

                when (rubric.type.uppercase()) {
                    "PROVENTO" -> totalEarnings = totalEarnings.add(itemValue)
                    "DESCONTO" -> totalDeductions = totalDeductions.add(itemValue)
                }
            }
        }

        // 7. Eventos de ponto como itens de holerite
        pontoEvents.forEach { ev ->
            val rubric = pontoPayrollIntegration.findOrCreateRubric(tenantId, ev.codigoRubrica)
            val valor = ev.valorTotal ?: BigDecimal.ZERO
            if (valor > BigDecimal.ZERO) {
                val item = PayslipItem(
                    tenantId = tenantId,
                    payslip = Payslip(tenantId = tenantId, employeeId = employeeId, contractId = contractId, competence = competence),
                    rubric = rubric ?: PayrollRubric(
                        tenantId = tenantId, code = ev.codigoRubrica, description = ev.descricao ?: ev.codigoRubrica,
                        type = ev.tipo, calculationType = "FIXED", isActive = true
                    ),
                    description = ev.descricao ?: ev.codigoRubrica,
                    quantity = ev.quantidade ?: BigDecimal.ONE,
                    totalValue = valor,
                    type = ev.tipo
                )
                itemsToSave.add(item)
                when (ev.tipo.uppercase()) {
                    "PROVENTO" -> totalEarnings = totalEarnings.add(valor)
                    "DESCONTO" -> totalDeductions = totalDeductions.add(valor)
                }
            }
        }

        // Ajuste legado cobertura (se evento COBERTURA_PENALTY não gerou)
        if (pontoEvents.none { it.codigoRubrica == "COBERTURA_PENALTY" }) {
            val coverageAdjustment = if (coveragePercent < 95.0) {
                val penaltyPoints = ((95.0 - coveragePercent) / 5.0).toInt()
                baseSalary.multiply(BigDecimal("0.01")).multiply(BigDecimal(penaltyPoints))
            } else BigDecimal.ZERO
            if (coverageAdjustment > BigDecimal.ZERO) {
                totalDeductions = totalDeductions.add(coverageAdjustment)
            }
        }

        // Horas extras CLT: 50% (2 primeiras horas/dia agregadas) + 100% (excedente mensal)
        if (pontoEvents.none { it.codigoRubrica == "HORA_EXTRA" } && totalWorkedMinutes > 8800) {
            val heMinutos = totalWorkedMinutes - 8800
            val valorHora = baseSalary.divide(BigDecimal("220"), 4, java.math.RoundingMode.HALF_UP)
            val he50Min = heMinutos.coerceAtMost(120)
            val he100Min = (heMinutos - he50Min).coerceAtLeast(0)
            val valorHe50 = valorHora.multiply(BigDecimal("1.50"))
                .multiply(BigDecimal(he50Min).divide(BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP))
            val valorHe100 = valorHora.multiply(BigDecimal("2.00"))
                .multiply(BigDecimal(he100Min).divide(BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP))
            val totalHe = valorHe50.add(valorHe100)
            if (totalHe > BigDecimal.ZERO) {
                totalEarnings = totalEarnings.add(totalHe)
                itemsToSave.add(
                    PayslipItem(
                        tenantId = tenantId,
                        payslip = Payslip(tenantId = tenantId, employeeId = employeeId, contractId = contractId, competence = competence),
                        rubric = PayrollRubric(tenantId = tenantId, code = "HORA_EXTRA", description = "Horas extras 50%/100%", type = "PROVENTO", calculationType = "FIXED", isActive = true),
                        description = "HE 50% + 100%",
                        totalValue = totalHe,
                        type = "PROVENTO"
                    )
                )
            }
        }

        // Adicional noturno 20% (22h–05h estimado: 25% das HE ou 8% da base mensal)
        val adicionalNoturno = baseSalary.multiply(BigDecimal("0.08")).setScale(2, java.math.RoundingMode.HALF_UP)
        if (adicionalNoturno > BigDecimal.ZERO && pontoEvents.none { it.codigoRubrica == "ADICIONAL_NOTURNO" }) {
            totalEarnings = totalEarnings.add(adicionalNoturno)
            itemsToSave.add(
                PayslipItem(
                    tenantId = tenantId,
                    payslip = Payslip(tenantId = tenantId, employeeId = employeeId, contractId = contractId, competence = competence),
                    rubric = PayrollRubric(tenantId = tenantId, code = "ADICIONAL_NOTURNO", description = "Adicional noturno 20%", type = "PROVENTO", calculationType = "FIXED", isActive = true),
                    description = "Adicional noturno",
                    totalValue = adicionalNoturno,
                    type = "PROVENTO"
                )
            )
        }

        // DSR sobre HE + adicional noturno
        val baseDsr = itemsToSave.filter { it.rubric.code in listOf("HORA_EXTRA", "ADICIONAL_NOTURNO") }
            .sumOf { it.totalValue }
        if (baseDsr > BigDecimal.ZERO) {
            val domingosMes = 4
            val diasUteis = 22
            val dsr = baseDsr.divide(BigDecimal(diasUteis), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal(domingosMes)).setScale(2, java.math.RoundingMode.HALF_UP)
            if (dsr > BigDecimal.ZERO) {
                totalEarnings = totalEarnings.add(dsr)
                itemsToSave.add(
                    PayslipItem(
                        tenantId = tenantId,
                        payslip = Payslip(tenantId = tenantId, employeeId = employeeId, contractId = contractId, competence = competence),
                        rubric = PayrollRubric(tenantId = tenantId, code = "DSR", description = "DSR sobre HE/noturno", type = "PROVENTO", calculationType = "FIXED", isActive = true),
                        description = "DSR reflexo",
                        totalValue = dsr,
                        type = "PROVENTO"
                    )
                )
            }
        }

        // 8. INSS progressivo + IRRF tabela 2026
        val inss = BrazilPayrollTaxTables.calculateInss2026(totalEarnings)
        val irrf = BrazilPayrollTaxTables.calculateIrrf2026(
            grossEarnings = totalEarnings,
            inss = inss,
            dependents = 0,
            otherDeductions = totalDeductions
        )
        totalDeductions = totalDeductions.add(inss).add(irrf)

        val netAmount = totalEarnings.subtract(totalDeductions).max(BigDecimal.ZERO)

        // 9. Criar / atualizar Payslip
        val payslip = existing ?: Payslip(
            tenantId = tenantId,
            employeeId = employeeId,
            contractId = contractId,
            competence = competence
        )

        payslip.baseSalary = baseSalary
        payslip.totalEarnings = totalEarnings
        payslip.totalDeductions = totalDeductions
        payslip.netAmount = netAmount
        payslip.status = "CALCULATED"
        val feriasBaseRef = totalEarnings.divide(BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP)
        val provisaoFeriasRef = feriasBaseRef.add(feriasBaseRef.divide(BigDecimal("3"), 2, java.math.RoundingMode.HALF_UP))
        payslip.notes = "Salário ${if (salaryChangeEvents.isNotEmpty()) "atualizado" else "base"} + rubricas + cobertura ${coveragePercent.toInt()}% | Prov. férias ref.: R$ $provisaoFeriasRef"

        val savedPayslip = payslipRepository.save(payslip)

        // 10. Salvar os itens do holerite de forma mais completa
        payslipItemRepository.deleteByPayslipId(savedPayslip.id!!)

        itemsToSave.forEach { item ->
            item.payslip = savedPayslip
            payslipItemRepository.save(item)
        }

        // Adiciona item de INSS como desconto explícito
        val inssItem = PayslipItem(
            tenantId = tenantId,
            payslip = savedPayslip,
            rubric = activeRubrics.firstOrNull { it.code == "INSS" } ?: PayrollRubric(
                tenantId = tenantId, code = "INSS", description = "INSS", type = "DESCONTO",
                calculationType = "FIXED", isActive = true
            ),
            description = "Contribuição Previdenciária",
            totalValue = inss,
            type = "DESCONTO"
        )
        payslipItemRepository.save(inssItem)

        val irrfItem = PayslipItem(
            tenantId = tenantId,
            payslip = savedPayslip,
            rubric = activeRubrics.firstOrNull { it.code == "IRRF" } ?: PayrollRubric(
                tenantId = tenantId, code = "IRRF", description = "IRRF", type = "DESCONTO",
                calculationType = "FIXED", isActive = true
            ),
            description = "Imposto de Renda Retido na Fonte",
            totalValue = irrf,
            type = "DESCONTO"
        )
        payslipItemRepository.save(irrfItem)

        return savedPayslip
    }
}