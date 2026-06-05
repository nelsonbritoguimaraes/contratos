package com.contractops.api.ia.agents

import com.contractops.api.document.service.DocumentService
import com.contractops.api.document.service.OcrService
import com.contractops.api.ia.config.AiProperties
import com.contractops.api.ia.orchestrator.AiOrchestrator
import com.contractops.api.ia.service.IaApprovalQueueService
import com.contractops.api.ia.domain.IaApprovalQueueItem
import org.springframework.stereotype.Service
import java.util.*

/**
 * Agente de Documentos — OCR + extração + vinculação.
 * Em modo production usa DocumentService/OcrService reais.
 */
@Service
class DocumentAgent(
    private val orchestrator: AiOrchestrator,
    private val aiProperties: AiProperties,
    private val documentService: DocumentService? = null,
    private val ocrService: OcrService? = null,
    private val approvalQueueService: IaApprovalQueueService? = null
) {

    fun analisarDocumento(
        textoExtraido: String,
        tipoDocumento: String,
        documentId: UUID? = null,
        tenantId: UUID? = null
    ): String {
        var texto = textoExtraido

        if (aiProperties.isProduction() && documentId != null && tenantId != null && documentService != null) {
            val doc = documentService.findById(documentId, tenantId)
            if (doc != null) {
                texto = ocrService?.extractText(doc.filePath ?: "", doc.mimeType) ?: textoExtraido
            }
        }

        val prompt = """
            Você recebeu o seguinte texto extraído de um documento ($tipoDocumento):
            
            $texto
            
            Tarefa:
            1. Extraia informações estruturadas importantes (datas, valores, CNPJs, cláusulas críticas).
            2. Identifique se há pendências ou riscos no documento.
            3. Sugira em qual entidade do sistema ele deveria ser vinculado (contrato, CCT, funcionário, etc.).
        """.trimIndent()

        val response = orchestrator.execute(prompt, tenantId = tenantId)

        if (aiProperties.isProduction() && tenantId != null && approvalQueueService != null) {
            approvalQueueService.enqueue(
                IaApprovalQueueItem(
                    tenantId = tenantId,
                    agentName = "DocumentAgent",
                    actionType = "VINCULAR_DOCUMENTO",
                    entityType = "DOCUMENT",
                    entityId = documentId,
                    title = "Vincular documento $tipoDocumento",
                    summary = response.take(500),
                    payload = texto.take(2000),
                    requestedBy = "DocumentAgent"
                )
            )
        }

        return response
    }
}
