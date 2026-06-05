package com.contractops.api.fiscal.esocial

import com.contractops.api.fiscal.config.FiscalMode
import com.contractops.api.fiscal.model.FiscalTransmitResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("local")
class StubEsocialGateway : EsocialGateway {
    override fun transmit(eventType: String, xmlPayload: String): FiscalTransmitResult {
        val protocol = "STUB-${eventType}-${UUID.randomUUID().toString().take(12).uppercase()}"
        return FiscalTransmitResult(
            success = true,
            protocolNumber = protocol,
            receiptNumber = "REC-STUB-${System.currentTimeMillis()}",
            statusCode = 200,
            message = "Envio simulado (modo STUB). XML gerado com ${xmlPayload.length} bytes.",
            mode = FiscalMode.STUB.name
        )
    }
}
