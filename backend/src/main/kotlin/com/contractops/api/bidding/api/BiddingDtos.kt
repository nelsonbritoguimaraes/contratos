package com.contractops.api.bidding.api

import com.contractops.api.bidding.domain.Bidding
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * DTO de entrada para criar ou atualizar uma Licitação.
 */
data class BiddingRequest(
    val processoNumero: String? = null,
    val editalNumero: String? = null,
    val modalidade: String? = null,
    val portalOrigem: String? = null,
    val orgao: String,
    val cnpjOrgao: String? = null,
    val objeto: String? = null,
    val dataPublicacao: LocalDate? = null,
    val dataSessao: LocalDate? = null,
    val dataHomologacao: LocalDate? = null,
    val dataAdjudicacao: LocalDate? = null,
    val valorEstimado: BigDecimal? = null,
    val valorVencedor: BigDecimal? = null,
    val status: String = "HOMOLOGADA",
    val fonteRecurso: String? = null,
    val editalUrl: String? = null,
    val vencedorEmpresa: String? = null,
    val unidadeCompradora: String? = null,
    val regimeLegal: String? = null,
    val equipeResponsavel: String? = null,
    val garantiaProposta: BigDecimal? = null,
    val riscosIdentificados: String? = null,
    val linksExternos: String? = null,
    val pncpId: String? = null,
    val numeroAta: String? = null
)

/**
 * DTO de listagem (versão mais leve).
 */
data class BiddingListResponse(
    val id: UUID?,
    val editalNumero: String?,
    val orgao: String,
    val status: String,
    val valorVencedor: BigDecimal?,
    val dataHomologacao: LocalDate?
) {
    companion object {
        fun fromEntity(entity: Bidding): BiddingListResponse {
            return BiddingListResponse(
                id = entity.id,
                editalNumero = entity.editalNumero,
                orgao = entity.orgao,
                status = entity.status,
                valorVencedor = entity.valorVencedor,
                dataHomologacao = entity.dataHomologacao
            )
        }
    }
}
