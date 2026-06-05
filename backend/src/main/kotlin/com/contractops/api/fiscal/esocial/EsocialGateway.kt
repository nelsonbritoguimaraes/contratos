package com.contractops.api.fiscal.esocial

import com.contractops.api.fiscal.model.FiscalTransmitResult

interface EsocialGateway {
    fun transmit(eventType: String, xmlPayload: String): FiscalTransmitResult
}
