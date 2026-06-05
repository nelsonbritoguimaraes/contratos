package com.contractops.api.glosa.api

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class CalculateGlosasRequest(
    val contractId: UUID,
    val period: LocalDate   // ex: 2025-05-01
)

data class GlosaResponse(
    val id: UUID?,
    val contractId: UUID,
    val measurementPeriod: LocalDate,
    val glosaType: String,
    val description: String?,
    val glosaAmount: BigDecimal,
    val status: String
)

data class UpdateGlosaRequest(
    val status: String,
    val description: String? = null,
    val evidenceUrl: String? = null
)

data class GlosaAppealRequest(
    val motivo: String,
    val evidenceUrl: String? = null
)
