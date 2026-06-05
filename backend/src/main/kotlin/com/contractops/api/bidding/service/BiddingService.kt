package com.contractops.api.bidding.service

import com.contractops.api.bidding.api.BiddingListResponse
import com.contractops.api.bidding.api.BiddingRequest
import com.contractops.api.bidding.api.BiddingResponse
import com.contractops.api.bidding.domain.Bidding
import com.contractops.api.bidding.repository.BiddingRepository
import com.contractops.api.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BiddingService(
    private val biddingRepository: BiddingRepository,
    private val workflowService: BiddingWorkflowService
) {

    fun findAllByTenant(tenantId: UUID): List<BiddingListResponse> =
        biddingRepository.findByTenantId(tenantId)
            .map { BiddingListResponse.fromEntity(it) }

    fun findById(id: UUID): BiddingResponse? =
        biddingRepository.findById(id).orElse(null)
            ?.let { BiddingResponse.fromEntity(it) }

    @Transactional
    fun createBidding(tenantId: UUID, request: BiddingRequest): BiddingResponse {
        val bidding = Bidding(
            tenantId = tenantId,
            processoNumero = request.processoNumero,
            editalNumero = request.editalNumero,
            modalidade = request.modalidade,
            portalOrigem = request.portalOrigem,
            orgao = request.orgao,
            cnpjOrgao = request.cnpjOrgao,
            objeto = request.objeto,
            dataPublicacao = request.dataPublicacao,
            dataSessao = request.dataSessao,
            dataHomologacao = request.dataHomologacao,
            dataAdjudicacao = request.dataAdjudicacao,
            valorEstimado = request.valorEstimado,
            valorVencedor = request.valorVencedor,
            status = request.status,
            fonteRecurso = request.fonteRecurso,
            editalUrl = request.editalUrl,
            vencedorEmpresa = request.vencedorEmpresa,
            unidadeCompradora = request.unidadeCompradora,
            regimeLegal = request.regimeLegal,
            equipeResponsavel = request.equipeResponsavel,
            garantiaProposta = request.garantiaProposta,
            riscosIdentificados = request.riscosIdentificados,
            linksExternos = request.linksExternos,
            pncpId = request.pncpId,
            numeroAta = request.numeroAta
        )

        val saved = biddingRepository.save(bidding)
        return BiddingResponse.fromEntity(saved)
    }

    @Transactional
    fun updateBidding(id: UUID, tenantId: UUID, request: BiddingRequest): BiddingResponse {
        val existing = biddingRepository.findById(id).orElse(null)
            ?: throw ResourceNotFoundException("Licitação não encontrada: $id")

        if (existing.tenantId != tenantId) {
            throw IllegalArgumentException("Licitação não pertence ao tenant")
        }

        request.editalNumero?.let { existing.editalNumero = it }
        request.processoNumero?.let { existing.processoNumero = it }
        request.modalidade?.let { existing.modalidade = it }
        request.portalOrigem?.let { existing.portalOrigem = it }
        request.orgao?.let { existing.orgao = it }
        request.cnpjOrgao?.let { existing.cnpjOrgao = it }
        request.objeto?.let { existing.objeto = it }
        request.dataPublicacao?.let { existing.dataPublicacao = it }
        request.dataSessao?.let { existing.dataSessao = it }
        request.dataHomologacao?.let { existing.dataHomologacao = it }
        request.dataAdjudicacao?.let { existing.dataAdjudicacao = it }
        request.valorEstimado?.let { existing.valorEstimado = it }
        request.valorVencedor?.let { existing.valorVencedor = it }
        request.fonteRecurso?.let { existing.fonteRecurso = it }
        request.status?.let { existing.status = it }
        applyExtended(existing, request)

        val saved = biddingRepository.save(existing)
        return BiddingResponse.fromEntity(saved)
    }

    @Transactional
    fun transitionStatus(id: UUID, tenantId: UUID, novoStatus: String): BiddingResponse {
        val existing = biddingRepository.findById(id).orElse(null)
            ?: throw ResourceNotFoundException("Licitação não encontrada: $id")
        if (existing.tenantId != tenantId) throw IllegalArgumentException("Licitação não pertence ao tenant")
        workflowService.validateTransition(existing.status, novoStatus)
        existing.status = novoStatus.uppercase()
        return BiddingResponse.fromEntity(biddingRepository.save(existing))
    }

    fun listStatuses(): List<String> = workflowService.allStatuses()

    private fun applyExtended(existing: Bidding, request: BiddingRequest) {
        request.editalUrl?.let { existing.editalUrl = it }
        request.vencedorEmpresa?.let { existing.vencedorEmpresa = it }
        request.unidadeCompradora?.let { existing.unidadeCompradora = it }
        request.regimeLegal?.let { existing.regimeLegal = it }
        request.equipeResponsavel?.let { existing.equipeResponsavel = it }
        request.garantiaProposta?.let { existing.garantiaProposta = it }
        request.riscosIdentificados?.let { existing.riscosIdentificados = it }
        request.linksExternos?.let { existing.linksExternos = it }
        request.pncpId?.let { existing.pncpId = it }
        request.numeroAta?.let { existing.numeroAta = it }
    }

    fun existsByIdAndTenant(id: UUID, tenantId: UUID): Boolean {
        return biddingRepository.findById(id)
            .map { it.tenantId == tenantId }
            .orElse(false)
    }

    // Internal use only (for services in the same bounded context)
    fun getEntityByIdAndTenant(id: UUID, tenantId: UUID): Bidding? {
        val bidding = biddingRepository.findById(id).orElse(null)
        return if (bidding != null && bidding.tenantId == tenantId) bidding else null
    }
}
