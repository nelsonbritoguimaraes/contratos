package com.contractops.api.bidding.api

import com.contractops.api.bidding.domain.WinningSpreadsheet
import java.util.*

data class WinningSpreadsheetRequest(
    val biddingId: UUID? = null,
    val contractId: UUID? = null,
    val versao: Int = 1,
    val arquivoNome: String? = null,
    val arquivoUrl: String? = null,
    val memoriaCalculo: String? = null,
    val isVencedora: Boolean = false
)

data class WinningSpreadsheetResponse(
    val id: UUID?,
    val biddingId: UUID?,
    val contractId: UUID?,
    val versao: Int,
    val arquivoNome: String?,
    val arquivoUrl: String?,
    val memoriaCalculo: String?,
    val isVencedora: Boolean,
    val createdAt: String?,
    val updatedAt: String?
) {
    companion object {
        fun fromEntity(entity: WinningSpreadsheet): WinningSpreadsheetResponse {
            return WinningSpreadsheetResponse(
                id = entity.id,
                biddingId = entity.bidding?.id,
                contractId = entity.contractId,
                versao = entity.versao,
                arquivoNome = entity.arquivoNome,
                arquivoUrl = entity.arquivoUrl,
                memoriaCalculo = entity.memoriaCalculo,
                isVencedora = entity.isVencedora,
                createdAt = entity.createdAt?.toString(),
                updatedAt = entity.updatedAt?.toString()
            )
        }
    }
}
