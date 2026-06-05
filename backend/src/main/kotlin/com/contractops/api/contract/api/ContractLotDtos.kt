package com.contractops.api.contract.api

import com.contractops.api.contract.domain.ContractLot
import java.math.BigDecimal
import java.util.*

data class ContractLotRequest(
    val originalBiddingLotId: UUID? = null,
    val numeroLote: String? = null,
    val descricao: String? = null,
    val quantitativoPostos: Int = 0,
    val valorMensal: BigDecimal? = null,
    val valorGlobal: BigDecimal? = null
)

data class ContractLotResponse(
    val id: UUID?,
    val contractId: UUID?,
    val originalBiddingLotId: UUID?,
    val numeroLote: String?,
    val descricao: String?,
    val quantitativoPostos: Int,
    val valorMensal: BigDecimal?,
    val valorGlobal: BigDecimal?
) {
    companion object {
        fun fromEntity(entity: ContractLot): ContractLotResponse {
            return ContractLotResponse(
                id = entity.id,
                contractId = entity.contract?.id,
                originalBiddingLotId = entity.originalBiddingLot?.id,
                numeroLote = entity.numeroLote,
                descricao = entity.descricao,
                quantitativoPostos = entity.quantitativoPostos,
                valorMensal = entity.valorMensal,
                valorGlobal = entity.valorGlobal
            )
        }
    }
}
