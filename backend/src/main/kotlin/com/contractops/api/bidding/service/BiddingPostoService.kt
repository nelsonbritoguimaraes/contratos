package com.contractops.api.bidding.service

import com.contractops.api.bidding.api.BiddingPostoRequest
import com.contractops.api.bidding.api.BiddingPostoResponse
import com.contractops.api.bidding.domain.BiddingPosto
import com.contractops.api.bidding.repository.BiddingPostoRepository
import com.contractops.api.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BiddingPostoService(
    private val repository: BiddingPostoRepository,
    private val biddingService: BiddingService
) {

    fun findByBidding(biddingId: UUID, tenantId: UUID): List<BiddingPostoResponse> {
        ensureBidding(biddingId, tenantId)
        return repository.findByBiddingIdAndTenantId(biddingId, tenantId).map { BiddingPostoResponse.from(it) }
    }

    @Transactional
    fun create(biddingId: UUID, tenantId: UUID, request: BiddingPostoRequest): BiddingPostoResponse {
        val bidding = biddingService.getEntityByIdAndTenant(biddingId, tenantId)
            ?: throw ResourceNotFoundException("Licitação não encontrada")
        val posto = BiddingPosto(
            tenantId = tenantId,
            bidding = bidding,
            biddingLotId = request.biddingLotId,
            codigo = request.codigo,
            nome = request.nome,
            funcao = request.funcao,
            cbo = request.cbo,
            escala = request.escala,
            jornadaHoras = request.jornadaHoras,
            valorMensal = request.valorMensal,
            localExecucao = request.localExecucao,
            municipioExecucao = request.municipioExecucao,
            quantidade = request.quantidade
        )
        return BiddingPostoResponse.from(repository.save(posto))
    }

    @Transactional
    fun delete(postoId: UUID, tenantId: UUID) {
        val posto = repository.findById(postoId).orElseThrow { ResourceNotFoundException("Posto não encontrado") }
        if (posto.tenantId != tenantId) throw IllegalArgumentException("Posto não pertence ao tenant")
        repository.delete(posto)
    }

    private fun ensureBidding(biddingId: UUID, tenantId: UUID) {
        if (!biddingService.existsByIdAndTenant(biddingId, tenantId)) {
            throw ResourceNotFoundException("Licitação não encontrada")
        }
    }
}
