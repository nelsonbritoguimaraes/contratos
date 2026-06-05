package com.contractops.api.rh.service

import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.contabilidade.service.ContabilidadeService
import com.contractops.api.financeiro.service.FinanceiroService
import com.contractops.api.rh.domain.Payslip
import com.contractops.api.rh.repository.PayslipRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class PayslipService(
    private val repository: PayslipRepository,
    private val esocialService: EsocialService,
    private val employeeAssignmentRepository: EmployeeAssignmentRepository,
    private val payrollEsocialOrchestrator: PayrollEsocialOrchestratorService? = null,
    @Lazy private val contabilidadeService: ContabilidadeService? = null,
    private val financeiroService: FinanceiroService? = null,
    private val payrollProvisionService: PayrollProvisionService? = null
) {

    fun findById(id: UUID, tenantId: UUID): Payslip? =
        repository.findById(id).filter { it.tenantId == tenantId }.orElse(null)

    @Transactional
    fun approvePayslip(payslipId: UUID, tenantId: UUID): Payslip {
        val payslip = repository.findById(payslipId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Holerite não encontrado") }

        if (payslip.status == "APPROVED" || payslip.status == "EXPORTED") {
            throw IllegalStateException("Holerite já está aprovado ou exportado")
        }

        payslip.status = "APPROVED"

        val saved = repository.save(payslip)

        // 1. Gera automaticamente evento S-1200 no eSocial
        try {
            esocialService.generateS1200Remuneracao(
                employeeId = saved.employeeId,
                tenantId = tenantId,
                competencia = saved.competence
            )
        } catch (e: Exception) {
            println("Aviso: Falha ao gerar evento S-1200 automático: ${e.message}")
        }

        // 2. Lança automaticamente na Contabilidade (se o módulo estiver disponível)
        try {
            contabilidadeService?.lancarFolhaAprovada(
                payslipId = saved.id!!,
                tenantId = tenantId,
                contratoId = saved.contractId,
                valorTotal = saved.totalEarnings ?: saved.netAmount ?: BigDecimal.ZERO
            )
        } catch (e: Exception) {
            println("Aviso: Falha ao gerar lançamento contábil automático da folha: ${e.message}")
        }

        // 3. Cria automaticamente Contas a Pagar (líquido + encargos) no módulo Financeiro
        try {
            financeiroService?.let { fin ->
                val breakdown = mapOf(
                    "liquido" to (saved.netAmount ?: BigDecimal.ZERO),
                    "inss" to (saved.totalDeductions ?: BigDecimal.ZERO).multiply(BigDecimal("0.6")),
                    "fgts" to (saved.totalEarnings ?: BigDecimal.ZERO).multiply(BigDecimal("0.08")),
                    "irrf" to (saved.totalEarnings ?: BigDecimal.ZERO).multiply(BigDecimal("0.04"))
                )
                fin.criarContasAPagarDaFolhaAprovada(
                    payslipId = saved.id!!,
                    tenantId = tenantId,
                    contratoId = saved.contractId,
                    breakdown = breakdown
                )
            }
        } catch (e: Exception) {
            println("Aviso: Falha ao criar Contas a Pagar da folha no módulo Financeiro: ${e.message}")
        }

        try {
            payrollProvisionService?.provisionarAoFecharPayslip(saved, tenantId)
        } catch (e: Exception) {
            println("Aviso: Falha ao provisionar férias/13º: ${e.message}")
        }

        return saved
    }

    @Transactional
    fun markAsExported(payslipId: UUID, tenantId: UUID): Payslip {
        val payslip = findById(payslipId, tenantId) 
            ?: throw IllegalArgumentException("Holerite não encontrado")

        if (payslip.status != "APPROVED") {
            throw IllegalStateException("Só é possível exportar holerites aprovados")
        }

        payslip.status = "EXPORTED"
        return repository.save(payslip)
    }

    fun findByContractAndCompetence(contractId: UUID, competence: java.time.LocalDate, tenantId: UUID): List<Payslip> {
        return repository.findByTenantIdAndContractIdAndCompetence(tenantId, contractId, competence)
    }

    fun getApprovedNetAmount(employeeId: UUID, competence: java.time.LocalDate, tenantId: UUID): java.math.BigDecimal? {
        val payslip = repository.findByTenantIdAndEmployeeIdAndCompetence(tenantId, employeeId, competence)
        return if (payslip != null && (payslip.status == "APPROVED" || payslip.status == "EXPORTED")) {
            payslip.netAmount
        } else null
    }

    /**
     * Fecha a competência para um contrato inteiro de forma inteligente:
     * - Busca todos os colaboradores alocados no contrato (via EmployeeAssignment)
     * - Para cada um: calcula o holerite se não existir
     * - Aprova todos os holerites da competência
     * - Gera S-1200 em batch
     */
    @Transactional
    fun closeCompetenceForContract(
        contractId: UUID,
        competence: java.time.LocalDate,
        tenantId: UUID,
        calculationService: PayrollCalculationService,
        esocialService: EsocialService
    ): List<Payslip> {

        // 1. Buscar todos os colaboradores alocados no contrato
        val assignments = employeeAssignmentRepository.findByTenantIdAndContractId(tenantId, contractId)
            .filter { it.isActive }

        val closedPayslips = mutableListOf<Payslip>()

        assignments.forEach { assignment ->
            val employeeId = assignment.employeeId

            // 2. Verificar se já existe holerite
            var payslip = repository.findByTenantIdAndEmployeeIdAndCompetence(tenantId, employeeId, competence)

            if (payslip == null) {
                // 3. Calcular se não existir
                try {
                    payslip = calculationService.calculatePayslip(employeeId, contractId, competence, tenantId)
                } catch (e: Exception) {
                    println("Erro ao calcular holerite para employee $employeeId: ${e.message}")
                    return@forEach
                }
            }

            // 4. Aprovar
            if (payslip.status != "APPROVED" && payslip.status != "EXPORTED") {
                payslip.status = "APPROVED"
                payslip = repository.save(payslip)

                // 5. Gerar S-1200
                try {
                    esocialService.generateS1200Remuneracao(employeeId, tenantId, competence)
                } catch (e: Exception) {
                    println("Erro ao gerar S-1200: ${e.message}")
                }

                closedPayslips.add(payslip)
            }
        }

        // Integração forte com Módulo Financeiro + Contabilidade (Bloco A)
        try {
            financeiroService?.processarFechamentoFolhaCompleto(
                payslipsAprovados = closedPayslips,
                tenantId = tenantId,
                contratoId = contractId
            )
        } catch (e: Exception) {
            println("Aviso: Falha na orquestração Financeiro/Contabilidade: ${e.message}")
        }

        try {
            payrollEsocialOrchestrator?.fecharCompetenciaFiscal(
                tenantId = tenantId,
                competencia = competence,
                contractId = contractId,
                transmitirAutomatico = false
            )
        } catch (e: Exception) {
            println("Aviso: Falha orquestração eSocial/FGTS/Reinf: ${e.message}")
        }

        return closedPayslips
    }

    /**
     * Fechamento Mensal Geral (para todos os contratos do tenant)
     */
    @Transactional
    fun closeAllContractsForCompetence(
        competence: java.time.LocalDate,
        tenantId: UUID,
        calculationService: PayrollCalculationService,
        esocialService: EsocialService
    ): Int {
        // Busca todos os contratos do tenant (simplificado)
        val contracts = com.contractops.api.contract.repository.ContractRepository::class.java // placeholder

        // Abordagem simplificada: fecha todos os payslips do tenant na competência
        val allPayslips = repository.findByTenantIdAndCompetenceBetween(
            tenantId, 
            competence.withDayOfMonth(1), 
            competence.withDayOfMonth(competence.lengthOfMonth())
        )

        var count = 0
        val newlyApproved = mutableListOf<Payslip>()

        allPayslips.filter { it.status != "APPROVED" && it.status != "EXPORTED" }.forEach {
            it.status = "APPROVED"
            repository.save(it)
            try {
                esocialService.generateS1200Remuneracao(it.employeeId, tenantId, competence)
            } catch (e: Exception) {}
            newlyApproved.add(it)
            count++
        }

        // Integração forte com Financeiro + Contabilidade também no fechamento geral (Bloco A)
        if (newlyApproved.isNotEmpty()) {
            try {
                financeiroService?.processarFechamentoFolhaCompleto(
                    payslipsAprovados = newlyApproved,
                    tenantId = tenantId
                )
            } catch (e: Exception) {
                println("Aviso: Falha na orquestração Financeiro/Contabilidade no fechamento mensal geral: ${e.message}")
            }
        }

        return count
    }

    /**
     * Reabre uma competência (desfaz o fechamento)
     */
    @Transactional
    fun reopenCompetenceForContract(
        contractId: UUID,
        competence: java.time.LocalDate,
        tenantId: UUID
    ): Int {
        val payslips = repository.findByTenantIdAndContractIdAndCompetence(tenantId, contractId, competence)
            .filter { it.status == "APPROVED" || it.status == "EXPORTED" }

        var count = 0
        payslips.forEach {
            it.status = "CALCULATED"  // Volta para editável
            repository.save(it)
            count++
        }
        return count
    }
}