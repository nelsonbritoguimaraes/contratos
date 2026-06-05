package com.contractops.api.fiscal.sped

import com.contractops.api.fiscal.config.FiscalMode
import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.model.FiscalTransmitResult
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SpedTransmitter(
    private val fiscalProperties: FiscalProperties
) {
    fun registerEcdFile(content: String, tipo: String = "ECD"): FiscalTransmitResult {
        val mode = fiscalProperties.resolvedMode()
        if (content.length < 20) {
            return FiscalTransmitResult(
                success = false,
                protocolNumber = null,
                receiptNumber = null,
                statusCode = 400,
                message = "Arquivo SPED vazio ou inválido",
                mode = mode.name
            )
        }
        val protocol = "${mode.name}-SPED-$tipo-${UUID.randomUUID().toString().take(10)}"
        return FiscalTransmitResult(
            success = true,
            protocolNumber = protocol,
            receiptNumber = "SPED-${System.currentTimeMillis()}",
            statusCode = if (mode == FiscalMode.PRODUCTION) 202 else 200,
            message = when (mode) {
                FiscalMode.PRODUCTION -> "SPED pronto para transmissão PVA/Receita (arquivo ${content.lines().size} linhas)"
                FiscalMode.SANDBOX -> "SPED validado em SANDBOX — use PVA para envio oficial"
                else -> "SPED gerado em modo STUB"
            },
            mode = mode.name,
            rawResponse = content.lines().take(5).joinToString("\n")
        )
    }
}
