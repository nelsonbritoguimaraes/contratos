package com.contractops.api.bidding.api

import com.contractops.api.bidding.domain.BiddingLot
import java.math.BigDecimal
import java.util.*

data class BiddingLotRequest(
    val numeroLote: String? = null,
    val descricao: String? = null,
    val quantitativoPostos: Int = 0,
    val valorMensal: BigDecimal? = null,
    val valorAnual: BigDecimal? = null,
    val valorGlobal: BigDecimal? = null,
    val prazoMeses: Int? = null
)

data class BiddingLotResponse(
    val id: UUID?,
    val biddingId: UUID?,
    val numeroLote: String?,
    val descricao: String?,
    val quantitativoPostos: Int,
    val valorMensal: BigDecimal?,
    val valorAnual: BigDecimal?,
    val valorGlobal: BigDecimal?,
    val prazoMeses: Int?
) {
    companion object {
        fun fromEntity(entity: BiddingLot): BiddingLotResponse {
            return BiddingLotResponse(
                id = entity.id,
                biddingId = entity.bidding?.id,
                numeroLote = entity.numeroLote,
                descricao = entity.descricao,
                quantitativoPostos = entity.quantitativoPostos,
                valorMensal = entity.valorMensal,
                valorAnual = entity.valorAnual,
                valorGlobal = entity.valorGlobal,
                prazoMeses = entity.prazoMeses
            )
        }
    }
}
