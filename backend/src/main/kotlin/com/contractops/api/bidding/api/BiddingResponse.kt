package com.contractops.api.bidding.api

import com.contractops.api.bidding.domain.Bidding
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * DTO de resposta para Licitação.
 * Evita expor a entidade JPA diretamente nas APIs.
 */
data class BiddingResponse(
    val id: UUID?,
    val tenantId: UUID,
    val processoNumero: String?,
    val editalNumero: String?,
    val modalidade: String?,
    val portalOrigem: String?,
    val orgao: String,
    val cnpjOrgao: String?,
    val objeto: String?,
    val dataPublicacao: LocalDate?,
    val dataSessao: LocalDate?,
    val dataHomologacao: LocalDate?,
    val dataAdjudicacao: LocalDate?,
    val valorEstimado: BigDecimal?,
    val valorVencedor: BigDecimal?,
    val status: String,
    val fonteRecurso: String?,
    val editalUrl: String?,
    val vencedorEmpresa: String?,
    val unidadeCompradora: String?,
    val regimeLegal: String?,
    val equipeResponsavel: String?,
    val garantiaProposta: BigDecimal?,
    val riscosIdentificados: String?,
    val linksExternos: String?,
    val pncpId: String?,
    val numeroAta: String?,
    val createdAt: String?,
    val updatedAt: String?
) {
    companion object {
        fun fromEntity(entity: Bidding): BiddingResponse = BiddingResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            processoNumero = entity.processoNumero,
            editalNumero = entity.editalNumero,
            modalidade = entity.modalidade,
            portalOrigem = entity.portalOrigem,
            orgao = entity.orgao,
            cnpjOrgao = entity.cnpjOrgao,
            objeto = entity.objeto,
            dataPublicacao = entity.dataPublicacao,
            dataSessao = entity.dataSessao,
            dataHomologacao = entity.dataHomologacao,
            dataAdjudicacao = entity.dataAdjudicacao,
            valorEstimado = entity.valorEstimado,
            valorVencedor = entity.valorVencedor,
            status = entity.status,
            fonteRecurso = entity.fonteRecurso,
            editalUrl = entity.editalUrl,
            vencedorEmpresa = entity.vencedorEmpresa,
            unidadeCompradora = entity.unidadeCompradora,
            regimeLegal = entity.regimeLegal,
            equipeResponsavel = entity.equipeResponsavel,
            garantiaProposta = entity.garantiaProposta,
            riscosIdentificados = entity.riscosIdentificados,
            linksExternos = entity.linksExternos,
            pncpId = entity.pncpId,
            numeroAta = entity.numeroAta,
            createdAt = entity.createdAt?.toString(),
            updatedAt = entity.updatedAt?.toString()
        )
    }
}
