package com.contractops.api.contract.service

import com.contractops.api.glosa.repository.GlosaRepository
import com.contractops.api.implantation.repository.ContractImplantationRepository
import com.contractops.api.measurement.service.MeasurementService
import com.contractops.api.notification.repository.ContractNotificationRepository
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.repository.VolanteAssignmentRepository
import com.contractops.api.time.service.CoverageService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

@Service
class ContractDashboardService(
    private val contractService: ContractService,
    private val measurementService: MeasurementService,
    private val glosaRepository: GlosaRepository,
    private val postRepository: ServicePostRepository,
    private val notificationRepository: ContractNotificationRepository,
    private val implantationRepository: ContractImplantationRepository,
    private val coverageService: CoverageService,
    private val volanteAssignmentRepository: VolanteAssignmentRepository
) {
    fun getDashboard(contractId: UUID, tenantId: UUID, referenceDate: LocalDate = LocalDate.now()): Map<String, Any?> {
        val contract = contractService.findById(contractId)?.takeIf { it.tenantId == tenantId }
            ?: throw IllegalArgumentException("Contrato não encontrado")

        val ym = YearMonth.from(referenceDate)
        val periodStart = ym.atDay(1)
        val periodEnd = ym.atEndOfMonth()

        val medicoes = measurementService.findByContract(contractId, tenantId)
        val glosas = glosaRepository.findByTenantIdAndContractId(tenantId, contractId)
            .filter { YearMonth.from(it.measurementPeriod) == ym }
        val posts = postRepository.findByContractId(contractId).filter { it.tenantId == tenantId }
        val notificacoes = notificationRepository.findByTenantIdAndContractId(tenantId, contractId)
        val implantacao = implantationRepository.findByTenantIdAndContractId(tenantId, contractId)
        val todayCoverage = coverageService.getDailyCoverageSummary(contractId, referenceDate, tenantId)
        val volanteAssignments = volanteAssignmentRepository.findByTenantIdAndContractIdAndAssignmentDateBetween(
            tenantId, contractId, periodStart, periodEnd
        )

        val glosaTotal = glosas.fold(BigDecimal.ZERO) { acc, g -> acc + g.glosaAmount }
        val faturamentoTotal = medicoes.fold(BigDecimal.ZERO) { acc, m ->
            acc + (m.finalAmount ?: BigDecimal.ZERO)
        }
        val coveragePercent = (todayCoverage["coverage_percent"] as? Double) ?: 0.0

        return mapOf(
            "contractId" to contractId,
            "referenceDate" to referenceDate.toString(),
            "competencia" to periodStart.toString(),
            "numero" to contract.numero,
            "orgao" to contract.orgao,
            "valorMensal" to contract.valorMensal,
            "status" to contract.status,
            "postsCount" to posts.size,
            "postsAtivos" to posts.count { it.status == "ATIVO" },
            "medicoesCount" to medicoes.size,
            "faturamentoTotal" to faturamentoTotal,
            "glosasCount" to glosas.size,
            "glosaTotal" to glosaTotal,
            "notificacoesPendentes" to notificacoes.count { it.status == "PENDENTE" },
            "implantacaoStatus" to implantacao?.status,
            "coberturaHoje" to mapOf(
                "percent" to coveragePercent,
                "breakdown" to todayCoverage["status_breakdown"],
                "posts" to todayCoverage["post_coverage"]
            ),
            "volantes" to mapOf(
                "totalMes" to volanteAssignments.size,
                "pendentes" to volanteAssignments.count {
                    it.workflowStatus in listOf("FALTA_DETECTADA", "VOLANTE_ATRIBUIDO")
                },
                "concluidos" to volanteAssignments.count { it.workflowStatus == "CONCLUIDO" }
            ),
            "ultimaMedicao" to medicoes.maxByOrNull { it.period }?.let {
                mapOf("period" to it.period.toString(), "finalAmount" to it.finalAmount, "status" to it.status)
            },
            "glosasRecentes" to glosas.takeLast(5).map {
                mapOf("id" to it.id, "tipo" to it.glosaType, "valor" to it.glosaAmount, "status" to it.status)
            },
            "glosasByType" to glosas.groupBy { it.glosaType }.mapValues { (_, list) ->
                mapOf(
                    "count" to list.size,
                    "amount" to list.fold(BigDecimal.ZERO) { acc, g -> acc + g.glosaAmount }
                )
            }
        )
    }
}
