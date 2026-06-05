package com.contractops.api.contract.api

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/** Request para criação de Contrato */
data class CreateContractRequest(
    val companyId: UUID,
    val branchId: UUID? = null,
    val biddingId: UUID? = null,
    val winningSpreadsheetId: UUID? = null,
    val numero: String,
    val orgao: String,
    val cnpjOrgao: String? = null,
    val objeto: String? = null,
    val vigenciaInicio: LocalDate? = null,
    val vigenciaFim: LocalDate? = null,
    val valorMensal: BigDecimal? = null,
    val valorGlobal: BigDecimal? = null,
    val status: String = "ATIVO",
    val prepostoNome: String? = null,
    val gestorOrgao: String? = null,
    val fiscalTecnico: String? = null,
    val fiscalAdministrativo: String? = null,
    val regrasGlosa: String? = null,
    val regrasSubstituicao: String? = null,
    val regrasUniforme: String? = null,
    val regrasEquipamentos: String? = null,
    val regrasFaturamento: String? = null,
    val regrasPonto: String? = null,
    val regrasMedicao: String? = null
)

/** Request para atualização parcial de Contrato */
data class UpdateContractRequest(
    val numero: String? = null,
    val orgao: String? = null,
    val cnpjOrgao: String? = null,
    val objeto: String? = null,
    val vigenciaInicio: LocalDate? = null,
    val vigenciaFim: LocalDate? = null,
    val valorMensal: BigDecimal? = null,
    val valorGlobal: BigDecimal? = null,
    val status: String? = null,
    val prepostoNome: String? = null,
    val gestorOrgao: String? = null,
    val fiscalTecnico: String? = null,
    val fiscalAdministrativo: String? = null,
    val winningSpreadsheetId: UUID? = null,
    val regrasGlosa: String? = null,
    val regrasSubstituicao: String? = null,
    val regrasUniforme: String? = null,
    val regrasEquipamentos: String? = null,
    val regrasFaturamento: String? = null,
    val regrasPonto: String? = null,
    val regrasMedicao: String? = null
)

/** Response padrão de Contrato */
data class ContractResponse(
    val id: UUID?,
    val tenantId: UUID,
    val companyId: UUID,
    val branchId: UUID?,
    val biddingId: UUID?,
    val winningSpreadsheetId: UUID?,
    val winningSpreadsheet: WinningSpreadsheetSummary? = null,
    val numero: String,
    val orgao: String,
    val cnpjOrgao: String?,
    val objeto: String?,
    val vigenciaInicio: LocalDate?,
    val vigenciaFim: LocalDate?,
    val valorMensal: BigDecimal?,
    val valorGlobal: BigDecimal?,
    val status: String,
    val qtdPostosContratados: Int,
    val prepostoNome: String?,
    val gestorOrgao: String?,
    val fiscalTecnico: String?,
    val fiscalAdministrativo: String?,
    val regrasGlosa: String?,
    val regrasSubstituicao: String?,
    val regrasUniforme: String?,
    val regrasEquipamentos: String?,
    val regrasFaturamento: String?,
    val regrasPonto: String?,
    val regrasMedicao: String?
)

data class LinkWinningSpreadsheetRequest(
    val winningSpreadsheetId: UUID
)

/**
 * Resumo leve da Planilha Vencedora para ser embutido no ContractResponse.
 */
data class WinningSpreadsheetSummary(
    val id: UUID?,
    val versao: Int,
    val isVencedora: Boolean,
    val arquivoNome: String?
)

data class ContractAmendmentResponse(
    val id: UUID?,
    val amendmentNumber: String?,
    val type: String,
    val description: String?,
    val effectiveDate: java.time.LocalDate?,
    val newEndDate: java.time.LocalDate?,
    val valueChange: java.math.BigDecimal?,
    val newMonthlyValue: java.math.BigDecimal?,
    val status: String,
    val documentUrl: String?
) {
    companion object {
        fun fromEntity(e: com.contractops.api.contract.domain.ContractAmendment): ContractAmendmentResponse =
            ContractAmendmentResponse(
                id = e.id,
                amendmentNumber = e.amendmentNumber,
                type = e.type,
                description = e.description,
                effectiveDate = e.effectiveDate,
                newEndDate = e.newEndDate,
                valueChange = e.valueChange,
                newMonthlyValue = e.newMonthlyValue,
                status = e.status,
                documentUrl = e.documentUrl
            )
    }
}

/** Request para criação de Aditivo/Repactuação/Reajuste (SPEC §6.2) */
data class CreateAmendmentRequest(
    @field:jakarta.validation.constraints.Size(max = 50)
    val amendmentNumber: String? = null,

    @field:jakarta.validation.constraints.NotBlank
    val type: String,   // PRORROGACAO, ACRESIMO, SUPRESSAO, REPACTUACAO, REAJUSTE, REEQUILIBRIO

    val description: String? = null,

    val effectiveDate: LocalDate? = null,
    val newEndDate: LocalDate? = null,

    val valueChange: BigDecimal? = null,
    val newMonthlyValue: BigDecimal? = null,

    val status: String = "VIGENTE",
    val documentUrl: String? = null
)

/** Request simples para atualização de status ou campos principais de um aditivo */
data class UpdateAmendmentRequest(
    val amendmentNumber: String? = null,
    val description: String? = null,
    val effectiveDate: LocalDate? = null,
    val newEndDate: LocalDate? = null,
    val valueChange: BigDecimal? = null,
    val newMonthlyValue: BigDecimal? = null,
    val status: String? = null,
    val documentUrl: String? = null
)
