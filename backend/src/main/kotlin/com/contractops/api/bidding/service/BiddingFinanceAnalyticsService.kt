package com.contractops.api.bidding.service

import com.contractops.api.bidding.domain.Bidding
import com.contractops.api.bidding.repository.BiddingPostoRepository
import com.contractops.api.bidding.repository.BiddingProposalRepository
import com.contractops.api.bidding.repository.BiddingRepository
import com.contractops.api.common.exception.ResourceNotFoundException
import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.measurement.repository.MeasurementRepository
import com.contractops.api.rh.repository.PayslipRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

@Service
class BiddingFinanceAnalyticsService(
    private val biddingRepository: BiddingRepository,
    private val contractRepository: ContractRepository,
    private val postoRepository: BiddingPostoRepository,
    private val proposalRepository: BiddingProposalRepository,
    private val payslipRepository: PayslipRepository,
    private val measurementRepository: MeasurementRepository
) {

    fun drePorLicitcao(biddingId: UUID, tenantId: UUID, competencia: LocalDate? = null): Map<String, Any> {
        val bidding = biddingRepository.findById(biddingId).orElse(null)
            ?: throw ResourceNotFoundException("Licitação não encontrada")
        if (bidding.tenantId != tenantId) throw IllegalArgumentException("Licitação não pertence ao tenant")

        val comp = competencia?.withDayOfMonth(1) ?: LocalDate.now().withDayOfMonth(1)
        val contracts = contractRepository.findByTenantIdAndBidding_Id(tenantId, biddingId)

        val postos = postoRepository.findByBiddingIdAndTenantId(biddingId, tenantId)
        val custoPlanejadoMensal = postos.mapNotNull { it.valorMensal?.multiply(BigDecimal(it.quantidade)) }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        var custoFolhaReal = BigDecimal.ZERO
        var receitaMedida = BigDecimal.ZERO
        contracts.forEach { c ->
            val cid = c.id!!
            payslipRepository.findByTenantIdAndContractIdAndCompetence(tenantId, cid, comp)
                .forEach { ps ->
                    custoFolhaReal = custoFolhaReal.add(ps.netAmount ?: BigDecimal.ZERO)
                    custoFolhaReal = custoFolhaReal.add(
                        (ps.totalEarnings ?: BigDecimal.ZERO).multiply(BigDecimal("0.28"))
                    )
                }
            measurementRepository.findByTenantIdAndContractIdAndPeriod(tenantId, cid, comp)?.let { m ->
                receitaMedida = receitaMedida.add(m.finalAmount ?: m.baseValue ?: BigDecimal.ZERO)
            }
        }

        val valorLicitado = bidding.valorVencedor ?: bidding.valorEstimado ?: BigDecimal.ZERO
        val propostaVencedora = proposalRepository.findByBiddingIdAndTenantIdOrderByVersaoDesc(biddingId, tenantId)
            .firstOrNull()
        val receitaRef = if (receitaMedida > BigDecimal.ZERO) receitaMedida
        else contracts.mapNotNull { it.valorMensal }.fold(BigDecimal.ZERO, BigDecimal::add)

        val custoRef = if (custoFolhaReal > BigDecimal.ZERO) custoFolhaReal else custoPlanejadoMensal
        val margem = receitaRef.subtract(custoRef)
        val margemPct = if (receitaRef > BigDecimal.ZERO) {
            margem.multiply(BigDecimal("100")).divide(receitaRef, 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        return mapOf(
            "biddingId" to biddingId,
            "competencia" to comp.toString(),
            "valorLicitado" to valorLicitado,
            "receitaReferencia" to receitaRef,
            "custoPlanejadoMensal" to custoPlanejadoMensal,
            "custoFolhaReal" to custoFolhaReal,
            "margemEstimada" to margem,
            "margemPercentual" to margemPct,
            "contratosVinculados" to contracts.size,
            "propostaReferencia" to (propostaVencedora?.valorProposta ?: BigDecimal.ZERO),
            "linhas" to listOf(
                mapOf("conta" to "Receita (medição/contrato)", "valor" to receitaRef),
                mapOf("conta" to "Custo folha + encargos", "valor" to custoFolhaReal.negate()),
                mapOf("conta" to "Custo planejado (postos)", "valor" to custoPlanejadoMensal.negate()),
                mapOf("conta" to "Margem", "valor" to margem)
            )
        )
    }
}

@Service
class BiddingAllocationService(
    private val biddingService: BiddingService,
    private val postoRepository: BiddingPostoRepository,
    private val contractRepository: ContractRepository,
    private val postService: com.contractops.api.post.service.PostService,
    private val employeeAssignmentRepository: com.contractops.api.employee.repository.EmployeeAssignmentRepository
) {

    fun resumoAlocacao(biddingId: UUID, tenantId: UUID): Map<String, Any> {
        if (!biddingService.existsByIdAndTenant(biddingId, tenantId)) {
            throw ResourceNotFoundException("Licitação não encontrada")
        }
        val postosPlanejados = postoRepository.findByBiddingIdAndTenantId(biddingId, tenantId)
        val contracts = contractRepository.findByTenantIdAndBidding_Id(tenantId, biddingId)
        val contract = contracts.firstOrNull()

        val postosExecutados = contract?.id?.let { postService.findByContract(it) } ?: emptyList()
        val assignments = contract?.id?.let { cid ->
            employeeAssignmentRepository.findByTenantIdAndContractId(tenantId, cid)
                .filter { a -> a.isActive }
        } ?: emptyList()

        val alocacoes = postosPlanejados.map { pp ->
            val exec = postosExecutados.find { sp ->
                sp.nome.equals(pp.nome, ignoreCase = true) ||
                    (pp.funcao != null && sp.funcao?.equals(pp.funcao, ignoreCase = true) == true)
            }
            val assigned = exec?.id?.let { pid ->
                assignments.filter { it.postId == pid }
            } ?: emptyList()
            mapOf(
                "postoPlanejadoId" to pp.id,
                "nome" to pp.nome,
                "funcao" to pp.funcao,
                "localExecucao" to pp.localExecucao,
                "municipioExecucao" to pp.municipioExecucao,
                "valorMensalPlanejado" to pp.valorMensal,
                "postoExecutadoId" to exec?.id,
                "alocacoesAtivas" to assigned.size,
                "coberturaPct" to if (assigned.isNotEmpty()) 100 else 0,
                "status" to if (assigned.isNotEmpty()) "OK" else if (exec != null) "PARCIAL" else "DESCOBERTO"
            )
        }

        return buildMap {
            put("biddingId", biddingId)
            contract?.id?.let { put("contractId", it) }
            contract?.numero?.let { put("contractNumero", it) }
            put("postosPlanejados", postosPlanejados.size)
            put("postosExecutados", postosExecutados.size)
            put("alocacoesAtivas", assignments.size)
            put(
                "coberturaMediaPct",
                if (alocacoes.isEmpty()) 0 else alocacoes.map { it["coberturaPct"] as Int }.average().toInt()
            )
            put("linhas", alocacoes)
        }
    }
}
