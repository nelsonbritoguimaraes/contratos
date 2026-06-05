package com.contractops.api.post.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.post.service.PostService
import com.contractops.api.employee.api.AssignEmployeeRequest
import com.contractops.api.employee.api.EmployeeAssignmentResponse
import com.contractops.api.employee.service.EmployeeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * Alias SPEC §26: POST /posts/{id}/assign-employee
 */
@RestController
@RequestMapping("/api/posts")
class PostAssignmentController(
    private val employeeService: EmployeeService,
    private val postService: PostService
) {
    @PostMapping("/{postId}/assign-employee")
    fun assignEmployeeToPost(
        @PathVariable postId: UUID,
        @RequestBody request: AssignEmployeeToPostRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeAssignmentResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val post = postService.findById(postId) ?: throw IllegalArgumentException("Posto não encontrado")
        val contractId = request.contractId ?: post.contractId

        val created = employeeService.assignEmployee(
            effectiveTenant,
            request.employeeId,
            AssignEmployeeRequest(
                contractId = contractId,
                postId = postId,
                role = request.role ?: "TITULAR",
                startDate = request.startDate,
                endDate = request.endDate
            )
        )
        return ResponseEntity
            .created(URI.create("/api/posts/$postId/assign-employee/${created.id}"))
            .body(EmployeeAssignmentResponse.fromEntity(created))
    }
}

data class AssignEmployeeToPostRequest(
    val employeeId: UUID,
    val contractId: UUID? = null,
    val role: String? = "TITULAR",
    val startDate: java.time.LocalDate? = null,
    val endDate: java.time.LocalDate? = null
)
