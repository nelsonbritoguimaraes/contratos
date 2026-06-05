package com.contractops.api.common.dto

import java.time.OffsetDateTime

/**
 * Resposta padronizada de erro para todas as APIs.
 */
data class ErrorResponse(
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null
)
