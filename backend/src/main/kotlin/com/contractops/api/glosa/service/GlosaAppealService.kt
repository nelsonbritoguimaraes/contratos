package com.contractops.api.glosa.service

import com.contractops.api.glosa.domain.GlosaAppeal
import com.contractops.api.glosa.domain.GlosaEvidence
import com.contractops.api.glosa.repository.GlosaAppealRepository
import com.contractops.api.glosa.repository.GlosaEvidenceRepository
import com.contractops.api.glosa.repository.GlosaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
class GlosaAppealService(
    private val glosaAppealRepository: GlosaAppealRepository,
    private val glosaEvidenceRepository: GlosaEvidenceRepository,
    private val glosaRepository: GlosaRepository,
    private val glosaService: GlosaService
) {

    @Transactional
    fun submitAppeal(
        glosaId: UUID,
        tenantId: UUID,
        appealReason: String,
        submittedBy: String? = null
    ): GlosaAppeal {
        val glosa = glosaRepository.findById(glosaId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Glosa não encontrada") }

        glosaService.updateStatus(glosaId, tenantId, "CONTESTADA")

        return glosaAppealRepository.save(
            GlosaAppeal(
                tenantId = tenantId,
                glosaId = glosaId,
                appealReason = appealReason,
                appealStatus = "ABERTO",
                submittedBy = submittedBy
            )
        )
    }

    @Transactional
    fun reviewAppeal(
        appealId: UUID,
        tenantId: UUID,
        decision: String,
        reviewedBy: String,
        reviewNotes: String? = null
    ): GlosaAppeal {
        val appeal = glosaAppealRepository.findById(appealId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Recurso não encontrado") }

        val status = decision.uppercase()
        if (status !in listOf("DEFERIDO", "INDEFERIDO", "EM_ANALISE")) {
            throw IllegalArgumentException("Decisão inválida: $decision")
        }

        appeal.appealStatus = status
        appeal.reviewedBy = reviewedBy
        appeal.reviewedAt = OffsetDateTime.now()
        appeal.reviewNotes = reviewNotes

        when (status) {
            "DEFERIDO" -> glosaService.updateStatus(appeal.glosaId, tenantId, "RECUPERADA")
            "INDEFERIDO" -> glosaService.updateStatus(appeal.glosaId, tenantId, "MANTIDA")
            "EM_ANALISE" -> glosaService.updateStatus(appeal.glosaId, tenantId, "CONTESTADA")
        }

        return glosaAppealRepository.save(appeal)
    }

    @Transactional
    fun addEvidence(
        glosaId: UUID,
        tenantId: UUID,
        fileUrl: String,
        evidenceType: String? = null,
        description: String? = null,
        submittedBy: String? = null
    ): GlosaEvidence {
        glosaRepository.findById(glosaId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Glosa não encontrada") }

        return glosaEvidenceRepository.save(
            GlosaEvidence(
                tenantId = tenantId,
                glosaId = glosaId,
                evidenceType = evidenceType,
                fileUrl = fileUrl,
                description = description,
                submittedBy = submittedBy,
                status = "PENDENTE"
            )
        )
    }

    fun listAppealsByGlosa(glosaId: UUID, tenantId: UUID): List<GlosaAppeal> =
        glosaAppealRepository.findByTenantIdAndGlosaId(tenantId, glosaId)

    fun listEvidencesByGlosa(glosaId: UUID, tenantId: UUID): List<GlosaEvidence> =
        glosaEvidenceRepository.findByTenantIdAndGlosaId(tenantId, glosaId)
}
