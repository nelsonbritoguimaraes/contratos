package com.contractops.api.measurement.service

import com.contractops.api.contabilidade.service.ContabilidadeService
import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.financeiro.service.FinanceiroService
import com.contractops.api.glosa.service.GlosaEngine
import com.contractops.api.measurement.domain.Measurement
import com.contractops.api.measurement.repository.MeasurementRepository
import com.contractops.api.rh.service.PayslipService
import com.contractops.api.time.service.AttendanceProcessingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class MeasurementService(
    private val repository: MeasurementRepository,
    private val glosaEngine: GlosaEngine,
    private val attendanceService: AttendanceProcessingService,
    private val contractRepository: ContractRepository,
    private val payslipService: PayslipService? = null,
    private val contabilidadeService: ContabilidadeService? = null,   // Integração com Contabilidade
    private val financeiroService: FinanceiroService? = null          // Integração CFO - Fase 2+
) {

    fun findByContract(contractId: UUID, tenantId: UUID): List<Measurement> =
        repository.findByTenantIdAndContractId(tenantId, contractId)

    @Transactional
    fun calculateMeasurement(contractId: UUID, period: LocalDate, tenantId: UUID): Measurement {
        // 1. Gera glosas do período
        val glosas = glosaEngine.calculateAndSaveGlosas(contractId, period, tenantId)
        val glosaTotal = glosas.sumOf { it.glosaAmount ?: BigDecimal.ZERO }

        // 2. Pega resumo de cobertura/attendance (já rico)
        val coverage = attendanceService.getDailyCoverageSummary(contractId, period, tenantId)
        val coveragePercent = (coverage["coverage_percent"] as? Double) ?: 100.0

        // 3. Ajuste simples por cobertura (exemplo: -1% por ponto abaixo de 95%)
        val adjustment = if (coveragePercent < 95.0) BigDecimal("-0.01").multiply(BigDecimal.valueOf(100 - coveragePercent)) else BigDecimal.ZERO

        val contract = contractRepository.findById(contractId).orElse(null)
        var base = contract?.valorMensal ?: BigDecimal("10000")

        // Integração folha por contrato: soma líquidos aprovados na competência (quando PayslipService disponível)
        payslipService?.let { ps ->
            val approved = ps.findByContractAndCompetence(contractId, period, tenantId)
                .filter { it.status == "APPROVED" || it.status == "EXPORTED" }
            val sumNet = approved.mapNotNull { it.netAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
            if (sumNet > BigDecimal.ZERO) {
                base = sumNet
            }
        }

        val finalAmount = base.subtract(glosaTotal).add(adjustment)

        val measurement = Measurement(
            tenantId = tenantId,
            contractId = contractId,
            period = period,
            baseValue = base,
            glosaTotal = glosaTotal,
            coverageAdjustment = adjustment,
            finalAmount = finalAmount.max(BigDecimal.ZERO),
            status = "DRAFT",
            notes = "Gerado automaticamente com glosas + cobertura de ${coveragePercent.toInt()}%"
        )

        return repository.save(measurement)
    }

    /**
     * Aprova uma medição e lança automaticamente na contabilidade.
     */
    @Transactional
    fun approveMeasurement(measurementId: UUID, tenantId: UUID): Measurement {
        val measurement = repository.findById(measurementId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Medição não encontrada") }

        if (measurement.status == "APPROVED" || measurement.status == "INVOICED") {
            throw IllegalStateException("Medição já está aprovada ou faturada")
        }

        measurement.status = "APPROVED"
        val saved = repository.save(measurement)

        // Lançamento automático na contabilidade
        try {
            contabilidadeService?.lancarMedicaoAprovada(
                measurementId = saved.id!!,
                tenantId = tenantId,
                contratoId = saved.contractId,
                valorLiquido = saved.finalAmount ?: BigDecimal.ZERO
            )
        } catch (e: Exception) {
            LoggerFactory.getLogger(javaClass).warn("Falha ao gerar lançamento contábil automático da medição: ${e.message}")
        }

        // === INTEGRAÇÃO COM MÓDULO FINANCEIRO (CFO) ===
        // Cria Conta a Receber + emite NFS-e automaticamente
        try {
            financeiroService?.let { fin ->
                val finalAmount = saved.finalAmount ?: BigDecimal.ZERO
                val vencimento = saved.period.plusDays(30)
                val contract = contractRepository.findById(saved.contractId).orElse(null)
                val tomadorCnpj = contract?.cnpjOrgao
                    ?.filter { it.isDigit() }
                    ?.takeIf { it.length == 14 }
                    ?: "00000000000000"
                val glosaProvisao = saved.glosaTotal ?: BigDecimal.ZERO

                fin.criarContaAReceberDaMedicao(
                    measurementId = saved.id!!,
                    contratoId = saved.contractId,
                    valorBruto = saved.baseValue ?: finalAmount,
                    valorLiquido = finalAmount,
                    vencimento = vencimento,
                    tenantId = tenantId,
                    glosaProvisao = glosaProvisao,
                    tomadorCnpj = tomadorCnpj
                )

                if (finalAmount > BigDecimal.ZERO) {
                    fin.emitirNfs(
                        measurementId = saved.id!!,
                        contratoId = saved.contractId,
                        tomadorCnpj = tomadorCnpj,
                        valorServicos = saved.baseValue ?: finalAmount,
                        tenantId = tenantId
                    )
                }

                // Atualiza status da medição para INVOICED
                saved.status = "INVOICED"
                repository.save(saved)
            }
        } catch (e: Exception) {
            LoggerFactory.getLogger(javaClass).warn("Falha na integração financeira da medição: ${e.message}")
        }

        return saved
    }
}