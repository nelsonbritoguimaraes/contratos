package com.contractops.api.bidding.api

import com.contractops.api.bidding.integration.PncpClient
import com.contractops.api.bidding.service.*
import com.contractops.api.common.tenant.TenantContext
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/biddings")
class BiddingController(
    private val biddingService: BiddingService,
    private val biddingLotService: BiddingLotService,
    private val winningSpreadsheetService: WinningSpreadsheetService,
    private val postoService: BiddingPostoService,
    private val itemService: BiddingItemService,
    private val proposalService: BiddingProposalService,
    private val deadlineService: BiddingDeadlineService,
    private val documentService: BiddingDocumentService,
    private val impugnacaoService: BiddingImpugnacaoService,
    private val ataService: BiddingAtaService,
    private val pncpClient: PncpClient,
    private val spreadsheetImportService: WinningSpreadsheetImportService,
    private val certidaoGateway: com.contractops.api.bidding.integration.BiddingCertidaoGateway,
    private val financeAnalytics: BiddingFinanceAnalyticsService,
    private val allocationService: BiddingAllocationService
) {

    private fun tenant(tenantId: UUID?) =
        tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

    @GetMapping
    fun list(@RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(biddingService.findAllByTenant(tenant(tenantId)))

    @GetMapping("/statuses")
    fun statuses() = ResponseEntity.ok(biddingService.listStatuses())

    @GetMapping("/pncp/search")
    fun pncpSearch(
        @RequestParam(required = false) termo: String?,
        @RequestParam(required = false) cnpjOrgao: String?,
        @RequestParam(defaultValue = "1") pagina: Int
    ) = ResponseEntity.ok(pncpClient.searchContratacoes(termo, cnpjOrgao, pagina))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID) =
        biddingService.findById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping
    fun create(@RequestBody request: BiddingRequest, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<BiddingResponse> {
        val created = biddingService.createBidding(tenant(tenantId), request)
        return ResponseEntity.created(URI.create("/api/biddings/${created.id}")).body(created)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: BiddingRequest,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(biddingService.updateBidding(id, tenant(tenantId), request))

    @PostMapping("/{id}/status")
    fun transitionStatus(
        @PathVariable id: UUID,
        @RequestBody body: BiddingStatusTransitionRequest,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(biddingService.transitionStatus(id, tenant(tenantId), body.novoStatus))

    // --- Lotes ---
    @GetMapping("/{biddingId}/lots")
    fun getLots(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(biddingLotService.findByBidding(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/lots")
    fun createLot(
        @PathVariable biddingId: UUID,
        @RequestBody request: BiddingLotRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<BiddingLotResponse> {
        val created = biddingLotService.createLot(biddingId, tenant(tenantId), request)
        return ResponseEntity.created(URI.create("/api/biddings/$biddingId/lots/${created.id}")).body(created)
    }

    @GetMapping("/lots/{lotId}/items")
    fun getItems(@PathVariable lotId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(itemService.listByLot(lotId, tenant(tenantId)))

    @PostMapping("/lots/{lotId}/items")
    fun createItem(
        @PathVariable lotId: UUID,
        @RequestBody request: BiddingItemRequest,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(itemService.create(lotId, tenant(tenantId), request))

    // --- Postos planejados ---
    @GetMapping("/{biddingId}/postos")
    fun getPostos(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(postoService.findByBidding(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/postos")
    fun createPosto(
        @PathVariable biddingId: UUID,
        @RequestBody request: BiddingPostoRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<BiddingPostoResponse> {
        val created = postoService.create(biddingId, tenant(tenantId), request)
        return ResponseEntity.created(URI.create("/api/biddings/$biddingId/postos/${created.id}")).body(created)
    }

    @DeleteMapping("/postos/{postoId}")
    fun deletePosto(@PathVariable postoId: UUID, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<Void> {
        postoService.delete(postoId, tenant(tenantId))
        return ResponseEntity.noContent().build()
    }

    // --- Planilhas ---
    @GetMapping("/{biddingId}/winning-spreadsheets")
    fun getWinningSpreadsheets(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(winningSpreadsheetService.findByBidding(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/winning-spreadsheets")
    fun createWinningSpreadsheet(
        @PathVariable biddingId: UUID,
        @RequestBody request: WinningSpreadsheetRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<WinningSpreadsheetResponse> {
        val created = winningSpreadsheetService.create(tenant(tenantId), request.copy(biddingId = biddingId))
        return ResponseEntity.created(URI.create("/api/biddings/$biddingId/winning-spreadsheets/${created.id}")).body(created)
    }

    @PostMapping("/{biddingId}/winning-spreadsheets/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importSpreadsheet(
        @PathVariable biddingId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(defaultValue = "false") markVencedora: Boolean,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(spreadsheetImportService.importSpreadsheet(biddingId, tenant(tenantId), file, markVencedora))

    @GetMapping("/{biddingId}/certidoes")
    fun certidoes(
        @PathVariable biddingId: UUID,
        @RequestParam cnpj: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<com.contractops.api.bidding.integration.BiddingCertidaoGateway.CertidaoResult>> {
        val t = tenant(tenantId)
        if (!biddingService.existsByIdAndTenant(biddingId, t)) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(certidaoGateway.consultar(cnpj))
    }

    @GetMapping("/{biddingId}/dre")
    fun dre(
        @PathVariable biddingId: UUID,
        @RequestParam(required = false) competencia: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val comp = competencia?.let { java.time.LocalDate.parse(it).withDayOfMonth(1) }
        return ResponseEntity.ok(financeAnalytics.drePorLicitcao(biddingId, tenant(tenantId), comp))
    }

    @GetMapping("/{biddingId}/allocation-summary")
    fun allocationSummary(
        @PathVariable biddingId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(allocationService.resumoAlocacao(biddingId, tenant(tenantId)))

    // --- Propostas ---
    @GetMapping("/{biddingId}/proposals")
    fun getProposals(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(proposalService.list(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/proposals")
    fun createProposal(
        @PathVariable biddingId: UUID,
        @RequestBody request: BiddingProposalRequest,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(proposalService.create(biddingId, tenant(tenantId), request))

    // --- Prazos ---
    @GetMapping("/{biddingId}/deadlines")
    fun getDeadlines(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(deadlineService.list(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/deadlines")
    fun createDeadline(
        @PathVariable biddingId: UUID,
        @RequestBody request: BiddingDeadlineRequest,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(deadlineService.create(biddingId, tenant(tenantId), request))

    @PostMapping("/deadlines/{deadlineId}/concluir")
    fun concluirDeadline(@PathVariable deadlineId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(deadlineService.markDone(deadlineId, tenant(tenantId)))

    // --- Documentos / Edital ---
    @GetMapping("/{biddingId}/documents")
    fun getDocuments(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(documentService.list(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/edital-upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadEdital(
        @PathVariable biddingId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(documentService.uploadEdital(biddingId, tenant(tenantId), file))

    @GetMapping("/files/download")
    fun serveFile(@RequestParam path: String): ResponseEntity<FileSystemResource> {
        val filePath = documentService.resolveFile(path) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${filePath.fileName}\"")
            .body(FileSystemResource(filePath))
    }

    // --- Impugnações / Atas ---
    @GetMapping("/{biddingId}/impugnacoes")
    fun getImpugnacoes(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(impugnacaoService.list(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/impugnacoes")
    fun createImpugnacao(
        @PathVariable biddingId: UUID,
        @RequestBody request: BiddingImpugnacaoRequest,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(impugnacaoService.create(biddingId, tenant(tenantId), request))

    @GetMapping("/{biddingId}/atas")
    fun getAtas(@PathVariable biddingId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(ataService.list(biddingId, tenant(tenantId)))

    @PostMapping("/{biddingId}/atas")
    fun createAta(
        @PathVariable biddingId: UUID,
        @RequestBody request: BiddingAtaRequest,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(ataService.create(biddingId, tenant(tenantId), request))
}

@RestController
@RequestMapping("/api/winning-spreadsheets")
class WinningSpreadsheetController(
    private val winningSpreadsheetService: WinningSpreadsheetService
) {
    @PostMapping("/{id}/set-vencedora")
    fun setVencedora(@PathVariable id: UUID, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<WinningSpreadsheetResponse> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(winningSpreadsheetService.setVencedora(id, t))
    }
}
