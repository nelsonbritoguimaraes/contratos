package com.contractops.api.glosa.service

import com.contractops.api.contabilidade.service.ContabilidadeService
import com.contractops.api.common.events.DomainEventPublisher
import com.contractops.api.common.events.glosaCalculatedEvent
import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.glosa.domain.Glosa
import com.contractops.api.glosa.repository.GlosaRepository
import com.contractops.api.glosa.repository.GlosaRuleRepository
import com.contractops.api.glosa.repository.IMRIndicatorRepository
import com.contractops.api.ia.agents.GlosaAgent
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.repository.VolanteAssignmentRepository
import com.contractops.api.time.service.CoverageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

/**
 * Motor de cálculo de Glosas e IMR (SPEC §17 e §18).
 * Usa apuração real de ponto (AttendanceDay) e cobertura operacional quando disponível.
 */
@Service
class GlosaEngine(
    private val glosaRuleRepository: GlosaRuleRepository,
    private val imrIndicatorRepository: IMRIndicatorRepository,
    private val glosaRepository: GlosaRepository,
    private val contractRepository: ContractRepository,
    private val attendanceDayRepository: AttendanceDayRepository,
    private val coverageService: CoverageService,
    private val servicePostRepository: ServicePostRepository,
    private val volanteAssignmentRepository: VolanteAssignmentRepository,
    private val glosaAgent: GlosaAgent? = null,
    private val contabilidadeService: ContabilidadeService? = null,
    private val domainEventPublisher: DomainEventPublisher? = null
) {

    @Transactional
    fun calculateAndSaveGlosas(contractId: UUID, period: LocalDate, tenantId: UUID): List<Glosa> {
        val contract = contractRepository.findById(contractId).orElseThrow {
            IllegalArgumentException("Contrato não encontrado")
        }

        if (contract.tenantId != tenantId) {
            throw IllegalArgumentException("Contrato não pertence ao tenant")
        }

        val ym = YearMonth.from(period)
        val periodStart = ym.atDay(1)
        val periodEnd = ym.atEndOfMonth()

        val attendances = attendanceDayRepository.findByTenantIdAndContractIdAndDateBetween(
            tenantId, contractId, periodStart, periodEnd
        )
        val expectedPosts = servicePostRepository.findByContractId(contractId).size.coerceAtLeast(1)
        val workingDays = ym.lengthOfMonth()

        val absenceDays = attendances.count { it.totalWorkedMinutes == 0 && it.absenceMinutes >= 240 }
        val uncoveredPostDays = countUncoveredPostDays(attendances, expectedPosts, periodStart, periodEnd)
        val totalDelayMinutes = attendances.sumOf { it.delayMinutes }
        val earlyExitDays = countEarlyExitDays(attendances)
        val partialCoverageDays = countPartialCoverageDays(tenantId, contractId, periodStart, periodEnd)
        val naoSubstituicaoDays = countNaoSubstituicaoDays(tenantId, contractId, periodStart, periodEnd)

        val coverageSummary = coverageService.getDailyCoverageSummary(contractId, periodEnd, tenantId)
        val coveragePercent = (coverageSummary["coverage_percent"] as? Double) ?: 100.0

        val rules = glosaRuleRepository.findByContractIdAndIsActiveTrue(contractId)
            .sortedBy { it.priority }
        val generatedGlosas = mutableListOf<Glosa>()
        val monthlyBase = contract.valorMensal ?: BigDecimal("10000")
        val dailyRate = monthlyBase.divide(BigDecimal(workingDays), 4, RoundingMode.HALF_UP)
        val hourlyRate = dailyRate.divide(BigDecimal(8), 4, RoundingMode.HALF_UP)

        rules.forEach { rule ->
            val factor = rule.factor ?: BigDecimal.ONE
            val tolerance = rule.toleranceMinutes ?: 0
            when (rule.ruleType.uppercase()) {
                "FALTA" -> {
                    if (absenceDays > 0) {
                        saveGlosa(
                            tenantId, contractId, period, rule.ruleType,
                            "Faltas apuradas no ponto: $absenceDays dia(s) no período",
                            monthlyBase,
                            dailyRate.multiply(BigDecimal(absenceDays)).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                        )
                    }
                }
                "ATRASO" -> {
                    val delayBeyondTolerance = (totalDelayMinutes - tolerance * attendances.size).coerceAtLeast(0)
                    if (delayBeyondTolerance > 0) {
                        val hours = BigDecimal(delayBeyondTolerance).divide(BigDecimal(60), 4, RoundingMode.HALF_UP)
                        saveGlosa(
                            tenantId, contractId, period, rule.ruleType,
                            "Atrasos apurados: ${delayBeyondTolerance}min (tolerância ${tolerance}min)",
                            monthlyBase,
                            hourlyRate.multiply(hours).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                        )
                    }
                }
                "SAIDA_ANTECIPADA" -> {
                    if (earlyExitDays > 0) {
                        saveGlosa(
                            tenantId, contractId, period, rule.ruleType,
                            "Saídas antecipadas: $earlyExitDays dia(s)",
                            monthlyBase,
                            dailyRate.multiply(BigDecimal(earlyExitDays)).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                        )
                    }
                }
                "POSTO_DESCOBERTO" -> {
                    if (uncoveredPostDays > 0) {
                        saveGlosa(
                            tenantId, contractId, period, rule.ruleType,
                            "Postos descobertos (dias-posto sem cobertura): $uncoveredPostDays",
                            monthlyBase,
                            dailyRate.multiply(BigDecimal(uncoveredPostDays)).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                        )
                    }
                }
                "COBERTURA_PARCIAL" -> {
                    if (partialCoverageDays > 0) {
                        saveGlosa(
                            tenantId, contractId, period, rule.ruleType,
                            "Cobertura parcial em $partialCoverageDays dia(s)-posto",
                            monthlyBase,
                            dailyRate.multiply(BigDecimal(partialCoverageDays)).multiply(BigDecimal("0.5"))
                                .multiply(factor).setScale(2, RoundingMode.HALF_UP)
                        )
                    }
                }
                "NAO_SUBSTITUICAO" -> {
                    if (naoSubstituicaoDays > 0) {
                        saveGlosa(
                            tenantId, contractId, period, rule.ruleType,
                            "Faltas sem substituição por volante: $naoSubstituicaoDays ocorrência(s)",
                            monthlyBase,
                            dailyRate.multiply(BigDecimal(naoSubstituicaoDays)).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                        )
                    }
                }
                "IMR" -> {
                    calculateImrGlosa(contractId, period, tenantId, monthlyBase, coveragePercent, factor)
                        ?.let { generatedGlosas.add(it) }
                }
                "AUSENCIA_DOCUMENTO" -> {
                    saveGlosa(
                        tenantId, contractId, period, rule.ruleType,
                        "Ausência de documento obrigatório — ${rule.description ?: "verificação pendente"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.01")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
                "UNIFORME", "UNIFORME_FALTANTE" -> {
                    saveGlosa(
                        tenantId, contractId, period, "UNIFORME",
                        "Glosa uniforme: ${rule.description ?: "item não conforme"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.02")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
                "EQUIPAMENTO", "EQUIPAMENTO_FALTANTE" -> {
                    saveGlosa(
                        tenantId, contractId, period, "EQUIPAMENTO",
                        "Glosa equipamento: ${rule.description ?: "item não conforme"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.02")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
                "QUALIDADE" -> {
                    saveGlosa(
                        tenantId, contractId, period, rule.ruleType,
                        "Glosa por qualidade: ${rule.description ?: "indicador abaixo da meta"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.03")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
                "DESCUMPRIMENTO_SLA" -> {
                    saveGlosa(
                        tenantId, contractId, period, rule.ruleType,
                        "Descumprimento de SLA: ${rule.description ?: "prazo excedido"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.05")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
                "NOTIFICACAO_NAO_RESPONDIDA" -> {
                    saveGlosa(
                        tenantId, contractId, period, rule.ruleType,
                        "Notificação não respondida: ${rule.description ?: "sem retorno no prazo"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.02")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
                "ADMINISTRATIVA" -> {
                    saveGlosa(
                        tenantId, contractId, period, rule.ruleType,
                        "Glosa administrativa: ${rule.description ?: "penalidade contratual"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.03")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
                "FINANCEIRA" -> {
                    saveGlosa(
                        tenantId, contractId, period, rule.ruleType,
                        "Glosa financeira: ${rule.description ?: "ajuste financeiro"}",
                        monthlyBase,
                        monthlyBase.multiply(BigDecimal("0.04")).multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    )
                }
            }
        }

        if (rules.none { it.ruleType.equals("IMR", ignoreCase = true) }) {
            val indicators = imrIndicatorRepository.findByContractIdAndIsActiveTrue(contractId)
            if (indicators.isNotEmpty()) {
                calculateImrFromIndicators(contractId, period, tenantId, monthlyBase, coveragePercent, indicators)
                    ?.let { generatedGlosas.add(it) }
            }
        }

        if (generatedGlosas.isEmpty() && coveragePercent < 95.0) {
            val shortfall = BigDecimal.valueOf(100.0 - coveragePercent)
                .divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            saveGlosa(
                tenantId, contractId, period, "COBERTURA",
                "Cobertura ${coveragePercent.toInt()}% abaixo de 95% (apuração de ponto)",
                monthlyBase,
                monthlyBase.multiply(shortfall).setScale(2, RoundingMode.HALF_UP)
            )
        }

        if (generatedGlosas.isEmpty() && !contract.regrasGlosa.isNullOrBlank()) {
            saveGlosa(
                tenantId, contractId, period, "REGRA_CONTRATO",
                "Glosa a partir de regras textuais do contrato",
                monthlyBase,
                monthlyBase.multiply(BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP)
            )
        }

        glosaAgent?.let { agent ->
            generatedGlosas.forEach { glosa ->
                try {
                    agent.analisarGlosa(
                        glosaDescricao = glosa.description ?: glosa.glosaType,
                        contratoContexto = "Contrato $contractId",
                        attendanceResumo = "Cobertura ${coveragePercent.toInt()}%, faltas $absenceDays"
                    )
                } catch (_: Exception) { /* IA stub — não bloqueia */ }
            }
        }

        generatedGlosas.forEach { glosa ->
            try {
                contabilidadeService?.lancarGlosaAplicada(
                    glosaId = glosa.id!!,
                    tenantId = tenantId,
                    contratoId = contractId,
                    valor = glosa.glosaAmount,
                    competencia = period
                )
            } catch (_: Exception) { /* não bloqueia apuração de glosa */ }

            domainEventPublisher?.publish(
                glosaCalculatedEvent(
                    tenantId = tenantId,
                    glosaId = glosa.id!!,
                    contractId = contractId,
                    glosaType = glosa.glosaType,
                    amount = glosa.glosaAmount,
                    period = period.toString()
                )
            )
        }

        return generatedGlosas
    }

    private fun countEarlyExitDays(attendances: List<com.contractops.api.time.domain.AttendanceDay>): Int =
        attendances.count { it.totalWorkedMinutes in 1..359 && it.absenceMinutes > 60 }

    private fun countPartialCoverageDays(
        tenantId: UUID,
        contractId: UUID,
        start: LocalDate,
        end: LocalDate
    ): Int {
        var count = 0
        var date = start
        while (!date.isAfter(end)) {
            val map = coverageService.getPostCoverageMap(tenantId, contractId, date)
            count += map.count { it["status"] == CoverageService.CoverageStatus.PARCIAL.name }
            date = date.plusDays(1)
        }
        return count
    }

    private fun countNaoSubstituicaoDays(
        tenantId: UUID,
        contractId: UUID,
        start: LocalDate,
        end: LocalDate
    ): Int {
        val assignments = volanteAssignmentRepository.findByTenantIdAndContractIdAndAssignmentDateBetween(
            tenantId, contractId, start, end
        )
        return assignments.count {
            it.workflowStatus in listOf("FALTA_DETECTADA", "CANCELADO") ||
                (it.workflowStatus == "VOLANTE_ATRIBUIDO" && it.volanteEmployeeId == null)
        }
    }

    private fun calculateImrGlosa(
        contractId: UUID,
        period: LocalDate,
        tenantId: UUID,
        monthlyBase: BigDecimal,
        coveragePercent: Double,
        factor: BigDecimal
    ): Glosa? {
        val indicators = imrIndicatorRepository.findByContractIdAndIsActiveTrue(contractId)
        return calculateImrFromIndicators(contractId, period, tenantId, monthlyBase, coveragePercent, indicators, factor)
    }

    private fun calculateImrFromIndicators(
        contractId: UUID,
        period: LocalDate,
        tenantId: UUID,
        monthlyBase: BigDecimal,
        coveragePercent: Double,
        indicators: List<com.contractops.api.glosa.domain.IMRIndicator>,
        factor: BigDecimal = BigDecimal.ONE
    ): Glosa? {
        if (indicators.isEmpty()) return null

        var weightedScore = BigDecimal.ZERO
        var totalWeight = BigDecimal.ZERO

        indicators.forEach { ind ->
            val weight = ind.weight
            val score = when (ind.measurementMethod?.uppercase()) {
                "PERCENTUAL_COBERTURA" -> BigDecimal.valueOf(coveragePercent)
                else -> BigDecimal.valueOf(coveragePercent)
            }
            weightedScore = weightedScore.add(score.multiply(weight))
            totalWeight = totalWeight.add(weight)
        }

        val imrScore = if (totalWeight > BigDecimal.ZERO) {
            weightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP)
        } else BigDecimal.valueOf(coveragePercent)

        val target = BigDecimal("95")
        if (imrScore >= target) return null

        val gap = target.subtract(imrScore).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        val amount = monthlyBase.multiply(gap).multiply(factor).setScale(2, RoundingMode.HALF_UP)
        if (amount <= BigDecimal.ZERO) return null

        return saveGlosa(
            tenantId, contractId, period, "IMR",
            "IMR ${imrScore}% (meta 95%) — ${indicators.size} indicador(es)",
            monthlyBase, amount
        )
    }

    private fun countUncoveredPostDays(
        attendances: List<com.contractops.api.time.domain.AttendanceDay>,
        expectedPosts: Int,
        start: LocalDate,
        end: LocalDate
    ): Int {
        var uncovered = 0
        var date = start
        while (!date.isAfter(end)) {
            val dayRecords = attendances.filter { it.date == date }
            val postsWithWork = dayRecords.count { it.totalWorkedMinutes > 0 }
            if (postsWithWork < expectedPosts) {
                uncovered += (expectedPosts - postsWithWork)
            }
            date = date.plusDays(1)
        }
        return uncovered
    }

    private fun saveGlosa(
        tenantId: UUID,
        contractId: UUID,
        period: LocalDate,
        type: String,
        description: String,
        baseValue: BigDecimal,
        amount: BigDecimal
    ): Glosa? {
        if (amount <= BigDecimal.ZERO) {
            return null
        }
        return glosaRepository.save(
            Glosa(
                tenantId = tenantId,
                contractId = contractId,
                measurementPeriod = period,
                glosaType = type,
                description = description,
                baseValue = baseValue,
                glosaAmount = amount,
                status = "APURADA"
            )
        )
    }

    fun getGlosasByPeriod(contractId: UUID, period: LocalDate, tenantId: UUID): List<Glosa> =
        glosaRepository.findByContractIdAndMeasurementPeriod(contractId, period)
            .filter { it.tenantId == tenantId }
}
