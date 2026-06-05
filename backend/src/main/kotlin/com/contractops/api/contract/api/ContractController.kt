package com.contractops.api.contract.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.contract.api.CreateAmendmentRequest
import com.contractops.api.contract.api.UpdateAmendmentRequest
import com.contractops.api.contract.domain.Contract
import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.contract.service.ContractLotService
import com.contractops.api.contract.service.ContractService
import com.contractops.api.post.api.CreatePostRequest
import com.contractops.api.post.api.UpdatePostRequest
import com.contractops.api.post.domain.ServicePost
import com.contractops.api.post.service.PostService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * ContractController — Fase 1 "Contrato Vivo"
 * Endpoints reais de CRUD para Contratos.
 *
 * Multi-tenancy ainda simplificado via query param (será trocado por extração do JWT).
 *
 * Alinhado com SPEC v1.0:
 * - Seção 6: Módulo de Contratos
 * - Seção 25: Modelo de dados (Contract, ContractLot, ServicePost)
 * - Seção 26.1: API de contratos
 */
@RestController
@RequestMapping("/api/contracts")
@PreAuthorize("hasAnyRole('ADMIN','GESTOR_GRUPO','GESTOR_CONTRATO','SUPERVISOR','FINANCEIRO','FISCAL_INTERNO','DP')")
class ContractController(
    private val contractService: ContractService,
    private val contractRepository: ContractRepository,
    private val postService: PostService,
    private val contractLotService: ContractLotService,
    private val contractAmendmentService: com.contractops.api.contract.service.ContractAmendmentService,
    private val contractDashboardService: com.contractops.api.contract.service.ContractDashboardService
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<List<ContractResponse>> {

        // Prefere o tenant vindo do TenantFilter (header X-Tenant-Id). Fallback para query param (dev) ou contexto.
        val effectiveTenant = tenantId
            ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val contracts = if (status != null) {
            contractRepository.findByTenantIdAndStatus(effectiveTenant, status)
        } else {
            contractService.findAllByTenant(effectiveTenant)
        }

        val response = contracts.map { toContractResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/dashboard")
    @PreAuthorize("@contractAccessService.canAccessContract(authentication, #id)")
    fun getDashboard(
        @PathVariable id: UUID,
        @RequestParam(required = false) referenceDate: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any?>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val date = referenceDate?.let { java.time.LocalDate.parse(it) } ?: java.time.LocalDate.now()
        return ResponseEntity.ok(contractDashboardService.getDashboard(id, effectiveTenant, date))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<ContractResponse> {
        return contractService.findById(id)
            ?.let { ResponseEntity.ok(toContractResponse(it)) }
            ?: ResponseEntity.notFound().build()
    }

    /**
     * Atualiza um contrato existente.
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateContractRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractResponse> {

        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val updated = contractService.updateContract(id, effectiveTenant) { c ->
            request.numero?.let { c.numero = it }
            request.orgao?.let { c.orgao = it }
            request.objeto?.let { c.objeto = it }
            request.vigenciaInicio?.let { c.vigenciaInicio = it }
            request.vigenciaFim?.let { c.vigenciaFim = it }
            request.valorMensal?.let { c.valorMensal = it }
            request.valorGlobal?.let { c.valorGlobal = it }
            request.status?.let { c.status = it }
            request.prepostoNome?.let { c.prepostoNome = it }
            request.gestorOrgao?.let { c.gestorOrgao = it }
            request.fiscalTecnico?.let { c.fiscalTecnico = it }
            request.fiscalAdministrativo?.let { c.fiscalAdministrativo = it }
            request.regrasGlosa?.let { c.regrasGlosa = it }
            request.regrasSubstituicao?.let { c.regrasSubstituicao = it }
            request.regrasUniforme?.let { c.regrasUniforme = it }
            request.regrasEquipamentos?.let { c.regrasEquipamentos = it }
            request.regrasFaturamento?.let { c.regrasFaturamento = it }
            request.regrasPonto?.let { c.regrasPonto = it }
            request.regrasMedicao?.let { c.regrasMedicao = it }
        }

        return if (updated != null) {
            ResponseEntity.ok(toContractResponse(updated))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Lista todos os postos de um contrato específico.
     * Essencial para o fluxo Contrato → Postos (master-detail).
     */
    @GetMapping("/{contractId}/posts")
    fun getPosts(@PathVariable contractId: UUID): ResponseEntity<List<ServicePost>> {
        val posts = postService.findByContract(contractId)
        return ResponseEntity.ok(posts)
    }

    /**
     * Cria um novo contrato.
     * Por enquanto aceita tenantId via query param para facilitar testes.
     */
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateContractRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractResponse> {

        val effectiveTenant = tenantId
            ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val created = contractService.createContract(
            tenantId = effectiveTenant,
            companyId = request.companyId,
            branchId = request.branchId,
            numero = request.numero,
            orgao = request.orgao,
            cnpjOrgao = request.cnpjOrgao,
            objeto = request.objeto,
            vigenciaInicio = request.vigenciaInicio,
            vigenciaFim = request.vigenciaFim,
            valorMensal = request.valorMensal,
            valorGlobal = request.valorGlobal,
            status = request.status,
            winningSpreadsheetId = request.winningSpreadsheetId,
            biddingId = request.biddingId
        )

        return ResponseEntity
            .created(URI.create("/api/contracts/${created.id}"))
            .body(toContractResponse(created))
    }

    /**
     * Cria um novo posto dentro de um contrato.
     * Rota: POST /api/contracts/{contractId}/posts
     */
    @PostMapping("/{contractId}/posts")
    fun createPost(
        @PathVariable contractId: UUID,
        @RequestBody request: CreatePostRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ServicePost> {

        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val created = postService.createPost(
            tenantId = effectiveTenant,
            contractId = contractId,
            nome = request.nome,
            codigo = request.codigo,
            funcao = request.funcao,
            escala = request.escala,
            cbo = request.cbo,
            jornadaHoras = request.jornadaHoras,
            valorMensal = request.valorMensal,
            valorDiario = request.valorDiario,
            titularNome = request.titularNome
        )

        return ResponseEntity
            .created(URI.create("/api/contracts/$contractId/posts/${created.id}"))
            .body(created)
    }

    /**
     * Atualiza um posto existente.
     */
    @PutMapping("/posts/{postId}")
    fun updatePost(
        @PathVariable postId: UUID,
        @RequestBody request: UpdatePostRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ServicePost> {

        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val updated = postService.updatePost(
            id = postId,
            tenantId = effectiveTenant,
            nome = request.nome,
            codigo = request.codigo,
            funcao = request.funcao,
            escala = request.escala,
            valorMensal = request.valorMensal,
            status = request.status
        )

        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Vincula uma Planilha Vencedora a um Contrato existente.
     * Completa o fluxo Licitação → Planilha Vencedora → Contrato.
     */
    @PutMapping("/{id}/winning-spreadsheet")
    fun linkWinningSpreadsheet(
        @PathVariable id: UUID,
        @RequestBody request: LinkWinningSpreadsheetRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractResponse> {

        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val updated = contractService.linkWinningSpreadsheet(id, request.winningSpreadsheetId, effectiveTenant)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(toContractResponse(updated))
    }

    // === Contract Amendments (SPEC 6.2) ===

    @GetMapping("/{contractId}/amendments")
    fun getAmendments(
        @PathVariable contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<ContractAmendmentResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val amendments = contractAmendmentService.findByContract(contractId, effectiveTenant)
        return ResponseEntity.ok(amendments.map { ContractAmendmentResponse.fromEntity(it) })
    }

    @PostMapping("/{contractId}/amendments")
    fun createAmendment(
        @PathVariable contractId: UUID,
        @Valid @RequestBody request: CreateAmendmentRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractAmendmentResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val created = contractAmendmentService.createAmendment(contractId, effectiveTenant, request)

        return ResponseEntity
            .created(URI.create("/api/contracts/$contractId/amendments/${created.id}"))
            .body(ContractAmendmentResponse.fromEntity(created))
    }

    @PutMapping("/amendments/{amendmentId}")
    fun updateAmendment(
        @PathVariable amendmentId: UUID,
        @Valid @RequestBody request: UpdateAmendmentRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractAmendmentResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val updated = contractAmendmentService.updateAmendment(amendmentId, effectiveTenant, request)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(ContractAmendmentResponse.fromEntity(updated))
    }

    @DeleteMapping("/amendments/{amendmentId}")
    fun deleteAmendment(
        @PathVariable amendmentId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Void> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val deleted = contractAmendmentService.deleteAmendment(amendmentId, effectiveTenant)
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    // === Contract Lots (nested) ===

    @GetMapping("/{contractId}/lots")
    fun getContractLots(
        @PathVariable contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<ContractLotResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(contractLotService.findByContract(contractId, effectiveTenant))
    }

    @PostMapping("/{contractId}/lots")
    fun createContractLot(
        @PathVariable contractId: UUID,
        @RequestBody request: ContractLotRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractLotResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val created = contractLotService.createLot(contractId, effectiveTenant, request)
        return ResponseEntity
            .created(URI.create("/api/contracts/$contractId/lots/${created.id}"))
            .body(created)
    }

    private fun toContractResponse(c: Contract): ContractResponse {
        val wsSummary = c.winningSpreadsheet?.let {
            WinningSpreadsheetSummary(
                id = it.id,
                versao = it.versao,
                isVencedora = it.isVencedora,
                arquivoNome = it.arquivoNome
            )
        }

        return ContractResponse(
            id = c.id,
            tenantId = c.tenantId,
            companyId = c.companyId,
            branchId = c.branchId,
            biddingId = c.bidding?.id,
            winningSpreadsheetId = c.winningSpreadsheet?.id,
            winningSpreadsheet = wsSummary,
            numero = c.numero,
            orgao = c.orgao,
            cnpjOrgao = c.cnpjOrgao,
            objeto = c.objeto,
            vigenciaInicio = c.vigenciaInicio,
            vigenciaFim = c.vigenciaFim,
            valorMensal = c.valorMensal,
            valorGlobal = c.valorGlobal,
            status = c.status,
            qtdPostosContratados = c.qtdPostosContratados,
            prepostoNome = c.prepostoNome,
            gestorOrgao = c.gestorOrgao,
            fiscalTecnico = c.fiscalTecnico,
            fiscalAdministrativo = c.fiscalAdministrativo,
            regrasGlosa = c.regrasGlosa,
            regrasSubstituicao = c.regrasSubstituicao,
            regrasUniforme = c.regrasUniforme,
            regrasEquipamentos = c.regrasEquipamentos,
            regrasFaturamento = c.regrasFaturamento,
            regrasPonto = c.regrasPonto,
            regrasMedicao = c.regrasMedicao
        )
    }
}


