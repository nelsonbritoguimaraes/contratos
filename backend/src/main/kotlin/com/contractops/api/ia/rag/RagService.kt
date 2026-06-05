package com.contractops.api.ia.rag

import com.contractops.api.contract.service.ContractService
import com.contractops.api.cct.service.CctService
import com.contractops.api.document.service.DocumentService
import com.contractops.api.ia.config.AiProperties
import com.contractops.api.ia.domain.RagIndexDocument
import com.contractops.api.ia.repository.RagIndexDocumentRepository
import com.contractops.api.measurement.service.MeasurementService
import com.contractops.api.rh.service.PayslipService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

/**
 * RAG Service — OpenSearch client stub with in-memory index fallback (Phase 7).
 */
@Service
class RagService(
    private val aiProperties: AiProperties,
    private val ragIndexRepository: RagIndexDocumentRepository,
    private val contractService: ContractService?,
    private val cctService: CctService?,
    private val documentService: DocumentService?,
    @Lazy private val measurementService: MeasurementService?,
    private val payslipService: PayslipService?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun buscarContextoContrato(contratoId: UUID, tenantId: UUID): String {
        val indexed = searchIndexed(tenantId, "CONTRACT", contratoId)
        if (indexed.isNotBlank()) return indexed

        val sb = StringBuilder()
        contractService?.let {
            val contrato = it.findById(contratoId)?.takeIf { c -> c.tenantId == tenantId }
            if (contrato != null) {
                sb.appendLine("=== CONTRATO ===")
                sb.appendLine("Número: ${contrato.numero}")
                sb.appendLine("Objeto: ${contrato.objeto}")
                sb.appendLine("Valor mensal: ${contrato.valorMensal}")
                sb.appendLine("Vigência: ${contrato.vigenciaInicio} a ${contrato.vigenciaFim}")
                sb.appendLine("Regras de glosa: ${contrato.regrasGlosa}")
                indexDocument(tenantId, "CONTRACT:$contratoId", "CONTRACT", contratoId, contrato.numero, sb.toString())
            }
        }

        measurementService?.let {
            val medicoes = it.findByContract(contratoId, tenantId).take(2)
            if (medicoes.isNotEmpty()) {
                sb.appendLine("\n=== MEDIÇÕES RECENTES ===")
                medicoes.forEach { m ->
                    sb.appendLine("- Período ${m.period}: Final ${m.finalAmount} | Glosas ${m.glosaTotal}")
                }
            }
        }

        documentService?.let {
            val docs = it.findByEntity("CONTRACT", contratoId, tenantId).take(3)
            if (docs.isNotEmpty()) {
                sb.appendLine("\n=== DOCUMENTOS ===")
                docs.forEach { d -> sb.appendLine("- ${d.title}") }
            }
        }

        return if (sb.isNotEmpty()) sb.toString() else "Nenhum contexto adicional encontrado."
    }

    fun buscarContextoFinanceiro(contratoId: UUID, tenantId: UUID): String {
        val sb = StringBuilder()
        measurementService?.let {
            val medicoes = it.findByContract(contratoId, tenantId).takeLast(3)
            if (medicoes.isNotEmpty()) {
                sb.appendLine("=== FATURAMENTO RECENTE ===")
                medicoes.forEach { m ->
                    sb.appendLine("- ${m.period}: Valor final R$${m.finalAmount} | Glosas R$${m.glosaTotal}")
                }
            }
        }
        return sb.toString().ifBlank { "Contexto financeiro não indexado." }
    }

    fun buscarContextoGeral(tenantId: UUID): String {
        val docs = ragIndexRepository.findByTenantIdOrderByIndexedAtDesc(tenantId).take(5)
        if (docs.isEmpty()) return "Contexto geral do tenant (índice vazio)."
        return docs.joinToString("\n") { "- ${it.title ?: it.docKey}: ${it.content.take(200)}" }
    }

    /** Semantic search stub — uses in-memory keyword match or OpenSearch when enabled. */
    fun search(query: String, tenantId: UUID, limit: Int = 5): List<RagIndexDocument> {
        if (aiProperties.opensearch.enabled) {
            log.debug("[RAG/OpenSearchStub] query='{}' host={}", query, aiProperties.opensearch.host)
            // Real client would call OpenSearch here
        }
        val q = query.lowercase()
        return ragIndexRepository.findByTenantIdOrderByIndexedAtDesc(tenantId)
            .filter { it.content.lowercase().contains(q) || (it.title?.lowercase()?.contains(q) == true) }
            .take(limit)
    }

    @Transactional
    fun indexDocument(
        tenantId: UUID,
        docKey: String,
        entityType: String?,
        entityId: UUID?,
        title: String?,
        content: String
    ): RagIndexDocument {
        val existing = ragIndexRepository.findByTenantIdAndDocKey(tenantId, docKey)
        val doc = existing ?: RagIndexDocument(
            tenantId = tenantId,
            docKey = docKey,
            entityType = entityType,
            entityId = entityId,
            title = title,
            content = content
        )
        doc.content = content
        doc.title = title
        doc.indexedAt = OffsetDateTime.now()
        if (aiProperties.opensearch.enabled) {
            doc.embeddingStub = "[opensearch-stub] dims=384 hash=${content.hashCode()}"
        }
        return ragIndexRepository.save(doc)
    }

    private fun searchIndexed(tenantId: UUID, entityType: String, entityId: UUID): String {
        val key = "$entityType:$entityId"
        return ragIndexRepository.findByTenantIdAndDocKey(tenantId, key)?.content ?: ""
    }
}
