package com.contractops.api.fiscal.esocial

import com.contractops.api.fiscal.config.FiscalMode
import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.model.FiscalTransmitResult
import org.springframework.stereotype.Service

@Service
class EsocialGatewayRouter(
    private val fiscalProperties: FiscalProperties,
    private val stubGateway: StubEsocialGateway,
    private val sandboxGateway: SandboxEsocialGateway,
    private val httpGateway: HttpEsocialGateway
) {
    fun transmit(eventType: String, xmlPayload: String): FiscalTransmitResult =
        when (fiscalProperties.resolvedMode()) {
            FiscalMode.STUB -> stubGateway.transmit(eventType, xmlPayload)
            FiscalMode.SANDBOX -> sandboxGateway.transmit(eventType, xmlPayload)
            FiscalMode.PRODUCTION -> httpGateway.transmit(eventType, xmlPayload)
        }
}
