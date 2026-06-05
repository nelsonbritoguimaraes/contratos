package com.contractops.api.fiscal.esocial

import com.contractops.api.fiscal.config.FiscalMode
import com.contractops.api.fiscal.model.FiscalTransmitResult
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Sandbox: valida XML mínimo e retorna protocolo realista sem chamar o governo.
 */
@Component
class SandboxEsocialGateway : EsocialGateway {
    override fun transmit(eventType: String, xmlPayload: String): FiscalTransmitResult {
        require(xmlPayload.contains("eSocial")) { "XML inválido: raiz eSocial ausente" }
        require(xmlPayload.contains("<Signature")) {
            "XML sem assinatura digital (<Signature>). Assine com certificado ICP-Brasil antes da transmissão."
        }
        val protocol = "SANDBOX-${eventType}-${UUID.randomUUID()}"
        return FiscalTransmitResult(
            success = true,
            protocolNumber = protocol,
            receiptNumber = "REC-SBX-${System.currentTimeMillis()}",
            statusCode = 200,
            message = "Aceito no ambiente SANDBOX (sem transmissão gov.br). Pronto para produção com certificado.",
            mode = FiscalMode.SANDBOX.name
        )
    }
}
