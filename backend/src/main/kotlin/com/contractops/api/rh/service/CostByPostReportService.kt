package com.contractops.api.rh.service

import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.rh.repository.PayslipRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

@Service
class CostByPostReportService(
    private val payslipRepository: PayslipRepository,
    private val assignmentRepository: EmployeeAssignmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val servicePostRepository: ServicePostRepository
) {
    data class CustoPostoLinha(
        val postId: UUID?,
        val postNome: String,
        val contractId: UUID,
        val headcount: Int,
        val totalBruto: BigDecimal,
        val totalLiquido: BigDecimal,
        val totalEncargos: BigDecimal,
        val custoMedioPorColaborador: BigDecimal
    )

    fun gerarRelatorio(contractId: UUID, competencia: LocalDate, tenantId: UUID): Map<String, Any> {
        val comp = competencia.withDayOfMonth(1)
        val fim = comp.withDayOfMonth(comp.lengthOfMonth())

        val payslips = payslipRepository.findByTenantIdAndContractIdAndCompetence(tenantId, contractId, comp)
            .filter { it.status in listOf("APPROVED", "EXPORTED", "CALCULATED") }

        val assignments = assignmentRepository.findByTenantIdAndContractId(tenantId, contractId)
            .filter { it.isActive }

        val posts = servicePostRepository.findByContractId(contractId)
        val postNames = posts.associate { it.id!! to (it.nome ?: it.codigo ?: "Posto") }

        val porPosto = mutableMapOf<UUID?, MutableList<UUID>>()
        assignments.forEach { a ->
            porPosto.getOrPut(a.postId) { mutableListOf() }.add(a.employeeId)
        }

        val linhas = mutableListOf<CustoPostoLinha>()
        var totalGeral = BigDecimal.ZERO

        porPosto.forEach { (postId, employeeIds) ->
            val slips = payslips.filter { it.employeeId in employeeIds }
            val bruto = slips.mapNotNull { it.totalEarnings }.fold(BigDecimal.ZERO, BigDecimal::add)
            val liquido = slips.mapNotNull { it.netAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
            val encargos = bruto.subtract(liquido).max(BigDecimal.ZERO)
            val hc = employeeIds.size.coerceAtLeast(1)
            val medio = liquido.divide(BigDecimal(hc), 2, RoundingMode.HALF_UP)

            linhas.add(
                CustoPostoLinha(
                    postId = postId,
                    postNome = postId?.let { postNames[it] } ?: "Contrato (sem posto)",
                    contractId = contractId,
                    headcount = hc,
                    totalBruto = bruto,
                    totalLiquido = liquido,
                    totalEncargos = encargos,
                    custoMedioPorColaborador = medio
                )
            )
            totalGeral = totalGeral.add(liquido)
        }

        if (linhas.isEmpty() && payslips.isNotEmpty()) {
            val bruto = payslips.mapNotNull { it.totalEarnings }.fold(BigDecimal.ZERO, BigDecimal::add)
            val liquido = payslips.mapNotNull { it.netAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
            linhas.add(
                CustoPostoLinha(
                    postId = null,
                    postNome = "Todos os postos",
                    contractId = contractId,
                    headcount = payslips.map { it.employeeId }.distinct().size,
                    totalBruto = bruto,
                    totalLiquido = liquido,
                    totalEncargos = bruto.subtract(liquido).max(BigDecimal.ZERO),
                    custoMedioPorColaborador = liquido.divide(
                        BigDecimal(payslips.size.coerceAtLeast(1)), 2, RoundingMode.HALF_UP
                    )
                )
            )
            totalGeral = liquido
        }

        return mapOf(
            "contractId" to contractId,
            "competencia" to comp.toString(),
            "periodoFim" to fim.toString(),
            "totalLiquido" to totalGeral,
            "linhas" to linhas.map {
                mapOf(
                    "postId" to it.postId,
                    "postNome" to it.postNome,
                    "headcount" to it.headcount,
                    "totalBruto" to it.totalBruto,
                    "totalLiquido" to it.totalLiquido,
                    "totalEncargos" to it.totalEncargos,
                    "custoMedioPorColaborador" to it.custoMedioPorColaborador
                )
            }
        )
    }
}
