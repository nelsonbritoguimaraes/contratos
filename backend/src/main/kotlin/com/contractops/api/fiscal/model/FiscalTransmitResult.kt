package com.contractops.api.fiscal.model

data class FiscalTransmitResult(
    val success: Boolean,
    val protocolNumber: String?,
    val receiptNumber: String?,
    val statusCode: Int?,
    val message: String,
    val mode: String,
    val rawResponse: String? = null
)
