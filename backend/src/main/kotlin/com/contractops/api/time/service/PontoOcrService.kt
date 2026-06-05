package com.contractops.api.time.service

import org.springframework.stereotype.Service

/**
 * OCR stub para folha de ponto física — extrai linhas texto para parser AFD delimitado.
 */
@Service
class PontoOcrService(
    private val afdImportService: AfdImportService
) {
    fun extrairMarcacoesDeTextoOcr(ocrText: String, deviceId: java.util.UUID?, tenantId: java.util.UUID) =
        afdImportService.parseAfdWithReport(ocrText, deviceId, tenantId)
}
