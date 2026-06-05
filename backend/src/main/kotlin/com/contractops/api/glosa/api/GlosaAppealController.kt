package com.contractops.api.glosa.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.glosa.domain.GlosaAppeal
import com.contractops.api.glosa.domain.GlosaEvidence
import com.contractops.api.glosa.service.GlosaAppealService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

@RestController
@RequestMapping("/api/glosa-appeals")
class GlosaAppealController(
    private val glosaAppealService: GlosaAppealService
) {

    private fun tenant(tenantId: UUID?) =
        tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

    @PostMapping
    fun submitAppeal(
        @RequestBody request: SubmitAppealRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<GlosaAppealResponse> {
        val tid = tenant(tenantId)
        val appeal = glosaAppealService.submitAppeal(
            glosaId = request.glosaId, tenantId = tid,
            appealReason = request.appealReason, submittedBy = request.submittedBy
        )
        return ResponseEntity.created(URI.create("/api/glosa-appeals/${appeal.id}"))
            .body(GlosaAppealResponse.from(appeal))
    }

    @PostMapping("/{id}/review")
    fun review(
        @PathVariable id: UUID,
        @RequestBody request: ReviewAppealRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<GlosaAppealResponse> {
        val tid = tenant(tenantId)
        val reviewed = glosaAppealService.reviewAppeal(
            appealId = id, tenantId = tid, decision = request.decision,
            reviewedBy = request.reviewedBy, reviewNotes = request.reviewNotes
        )
        return ResponseEntity.ok(GlosaAppealResponse.from(reviewed))
    }

    @GetMapping
    fun listByGlosa(
        @RequestParam glosaId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<GlosaAppealResponse>> {
        val tid = tenant(tenantId)
        return ResponseEntity.ok(glosaAppealService.listAppealsByGlosa(glosaId, tid).map { GlosaAppealResponse.from(it) })
    }

    @PostMapping("/evidences")
    fun addEvidence(
        @RequestBody request: AddGlosaEvidenceRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<GlosaEvidenceResponse> {
        val tid = tenant(tenantId)
        val evidence = glosaAppealService.addEvidence(
            glosaId = request.glosaId, tenantId = tid, fileUrl = request.fileUrl,
            evidenceType = request.evidenceType, description = request.description,
            submittedBy = request.submittedBy
        )
        return ResponseEntity.created(URI.create("/api/glosa-appeals/evidences/${evidence.id}"))
            .body(GlosaEvidenceResponse.from(evidence))
    }

    @GetMapping("/evidences")
    fun listEvidences(
        @RequestParam glosaId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<GlosaEvidenceResponse>> {
        val tid = tenant(tenantId)
        return ResponseEntity.ok(glosaAppealService.listEvidencesByGlosa(glosaId, tid).map { GlosaEvidenceResponse.from(it) })
    }
}

data class SubmitAppealRequest(val glosaId: UUID, val appealReason: String, val submittedBy: String? = null)
data class ReviewAppealRequest(val decision: String, val reviewedBy: String, val reviewNotes: String? = null)
data class AddGlosaEvidenceRequest(
    val glosaId: UUID, val fileUrl: String, val evidenceType: String? = null,
    val description: String? = null, val submittedBy: String? = null
)

data class GlosaAppealResponse(
    val id: UUID?, val glosaId: UUID, val appealReason: String, val appealStatus: String,
    val submittedBy: String?, val submittedAt: OffsetDateTime,
    val reviewedBy: String?, val reviewNotes: String?
) {
    companion object {
        fun from(a: GlosaAppeal) = GlosaAppealResponse(
            id = a.id, glosaId = a.glosaId, appealReason = a.appealReason, appealStatus = a.appealStatus,
            submittedBy = a.submittedBy, submittedAt = a.submittedAt, reviewedBy = a.reviewedBy, reviewNotes = a.reviewNotes
        )
    }
}

data class GlosaEvidenceResponse(
    val id: UUID?, val glosaId: UUID, val evidenceType: String?, val fileUrl: String?,
    val description: String?, val status: String
) {
    companion object {
        fun from(e: GlosaEvidence) = GlosaEvidenceResponse(
            id = e.id, glosaId = e.glosaId, evidenceType = e.evidenceType,
            fileUrl = e.fileUrl, description = e.description, status = e.status
        )
    }
}
