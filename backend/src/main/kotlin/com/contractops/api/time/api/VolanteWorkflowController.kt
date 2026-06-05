package com.contractops.api.time.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.time.domain.VolanteAssignment
import com.contractops.api.time.service.CoverageService
import com.contractops.api.time.service.VolanteWorkflowService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/volantes")
class VolanteWorkflowController(
    private val volanteWorkflowService: VolanteWorkflowService,
    private val coverageService: CoverageService
) {

    private fun tenant(tenantId: UUID?) =
        tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

    @PostMapping("/detect")
    fun detect(
        @RequestParam contractId: UUID,
        @RequestParam date: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<VolanteAssignmentResponse>> {
        val tid = tenant(tenantId)
        val detected = volanteWorkflowService.detectAbsences(tid, contractId, LocalDate.parse(date))
        return ResponseEntity.ok(detected.map { VolanteAssignmentResponse.from(it) })
    }

    @PostMapping("/{id}/assign")
    fun assign(
        @PathVariable id: UUID,
        @RequestBody request: AssignVolanteRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<VolanteAssignmentResponse> {
        val tid = tenant(tenantId)
        val updated = volanteWorkflowService.assignVolante(id, tid, request.volanteEmployeeId, request.notes)
        return ResponseEntity.ok(VolanteAssignmentResponse.from(updated))
    }

    @PostMapping("/{id}/confirm")
    fun confirm(
        @PathVariable id: UUID,
        @RequestParam(required = false) confirmedBy: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<VolanteAssignmentResponse> {
        val tid = tenant(tenantId)
        val updated = volanteWorkflowService.confirmVolante(id, tid, confirmedBy)
        return ResponseEntity.ok(VolanteAssignmentResponse.from(updated))
    }

    @PostMapping("/{id}/evidence")
    fun evidence(
        @PathVariable id: UUID,
        @RequestBody request: VolanteEvidenceRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<VolanteAssignmentResponse> {
        val tid = tenant(tenantId)
        val updated = volanteWorkflowService.registerEvidence(id, tid, request.evidenceUrl, request.evidenceNotes)
        return ResponseEntity.ok(VolanteAssignmentResponse.from(updated))
    }

    @PostMapping("/{id}/complete")
    fun complete(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<VolanteAssignmentResponse> {
        val tid = tenant(tenantId)
        return ResponseEntity.ok(VolanteAssignmentResponse.from(volanteWorkflowService.completeAssignment(id, tid)))
    }

    @GetMapping
    fun list(
        @RequestParam contractId: UUID,
        @RequestParam date: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<VolanteAssignmentResponse>> {
        val tid = tenant(tenantId)
        val list = volanteWorkflowService.listAssignments(tid, contractId, LocalDate.parse(date))
        return ResponseEntity.ok(list.map { VolanteAssignmentResponse.from(it) })
    }

    @GetMapping("/suggest")
    fun suggest(
        @RequestParam contractId: UUID,
        @RequestParam postId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<Map<String, Any?>>> {
        val tid = tenant(tenantId)
        return ResponseEntity.ok(volanteWorkflowService.suggestVolantes(tid, contractId, postId))
    }

    @GetMapping("/coverage")
    fun coverage(
        @RequestParam contractId: UUID,
        @RequestParam date: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val tid = tenant(tenantId)
        return ResponseEntity.ok(coverageService.getDailyCoverageSummary(contractId, LocalDate.parse(date), tid))
    }
}

data class AssignVolanteRequest(val volanteEmployeeId: UUID, val notes: String? = null)
data class VolanteEvidenceRequest(val evidenceUrl: String, val evidenceNotes: String? = null)

data class VolanteAssignmentResponse(
    val id: UUID?,
    val contractId: UUID,
    val postId: UUID,
    val absentEmployeeId: UUID,
    val volanteEmployeeId: UUID?,
    val assignmentDate: LocalDate,
    val workflowStatus: String,
    val evidenceUrl: String?,
    val notes: String?
) {
    companion object {
        fun from(v: VolanteAssignment) = VolanteAssignmentResponse(
            id = v.id, contractId = v.contractId, postId = v.postId,
            absentEmployeeId = v.absentEmployeeId, volanteEmployeeId = v.volanteEmployeeId,
            assignmentDate = v.assignmentDate, workflowStatus = v.workflowStatus,
            evidenceUrl = v.evidenceUrl, notes = v.notes
        )
    }
}
