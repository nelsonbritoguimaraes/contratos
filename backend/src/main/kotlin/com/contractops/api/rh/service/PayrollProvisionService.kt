package com.contractops.api.rh.service

import com.contractops.api.contabilidade.service.ContabilidadeService
import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.rh.domain.Payslip
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

@Service
class PayrollProvisionService(
    private val contabilidadeService: ContabilidadeService? = null,
    private val contractRepository: ContractRepository? = null
) {
    @Transactional
    fun provisionarAoFecharPayslip(payslip: Payslip, tenantId: UUID) {
        val base = payslip.baseSalary ?: BigDecimal.ZERO
        val proventos = payslip.totalEarnings ?: base
        if (proventos <= BigDecimal.ZERO) return

        val feriasBase = proventos.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP)
        val tercoConstitucional = feriasBase.divide(BigDecimal("3"), 2, RoundingMode.HALF_UP)
        val ferias = feriasBase.add(tercoConstitucional)
        val decimo = proventos.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP)

        contabilidadeService?.let { contab ->
            val comp = payslip.competence.withDayOfMonth(1)
            val contract = contractRepository?.findById(payslip.contractId)?.orElse(null)

            contab.lancarProvisaoRH(
                tenantId = tenantId,
                contratoId = payslip.contractId,
                tipoProvisao = "FERIAS",
                valor = ferias,
                competencia = comp,
                branchId = contract?.branchId
            )
            contab.lancarProvisaoRH(
                tenantId = tenantId,
                contratoId = payslip.contractId,
                tipoProvisao = "13_SALARIO",
                valor = decimo,
                competencia = comp,
                branchId = contract?.branchId
            )
        }
    }
}
