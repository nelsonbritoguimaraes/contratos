package com.contractops.api.common.api

import com.contractops.api.common.domain.Company
import com.contractops.api.common.service.CompanyService
import com.contractops.api.common.tenant.TenantContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * CompanyController - Gestão de Empresas/CNPJs
 * Alinhado com SPEC v1.0 seções 4 e 25.1.
 */
@RestController
@RequestMapping("/api/companies")
class CompanyController(
    private val companyService: CompanyService
) {

    @GetMapping
    fun list(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<Company>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(companyService.findAllByTenant(effectiveTenant))
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateCompanyRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Company> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val created = companyService.create(effectiveTenant, request.cnpj, request.razaoSocial, request.nomeFantasia)
        return ResponseEntity
            .created(URI.create("/api/companies/${created.id}"))
            .body(created)
    }
}

data class CreateCompanyRequest(
    val cnpj: String,
    val razaoSocial: String,
    val nomeFantasia: String? = null
)
