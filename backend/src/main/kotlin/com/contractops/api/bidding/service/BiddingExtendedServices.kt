package com.contractops.api.bidding.service

import com.contractops.api.bidding.api.*
import com.contractops.api.bidding.domain.*
import com.contractops.api.bidding.repository.*
import com.contractops.api.common.exception.ResourceNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Service
class BiddingDocumentService(
    private val documentRepository: BiddingDocumentRepository,
    private val biddingRepository: com.contractops.api.bidding.repository.BiddingRepository,
    @Value("\${contractops.bidding.upload-dir:uploads/bidding}") private val uploadDir: String
) {

    fun list(biddingId: UUID, tenantId: UUID): List<BiddingDocumentResponse> {
        ensureBidding(biddingId, tenantId)
        return documentRepository.findByBiddingIdAndTenantIdOrderByCreatedAtDesc(biddingId, tenantId)
            .map { BiddingDocumentResponse.from(it) }
    }

    @Transactional
    fun uploadEdital(biddingId: UUID, tenantId: UUID, file: MultipartFile): Map<String, Any> {
        val bidding = ensureBidding(biddingId, tenantId)
        val dir = Paths.get(uploadDir, tenantId.toString(), biddingId.toString())
        Files.createDirectories(dir)
        val safeName = (file.originalFilename ?: "edital.pdf").replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target: Path = dir.resolve("${System.currentTimeMillis()}_$safeName")
        file.transferTo(target)

        val relative = target.toString().replace("\\", "/")
        val doc = BiddingDocument(
            tenantId = tenantId,
            biddingId = biddingId,
            tipo = "EDITAL",
            titulo = safeName,
            arquivoNome = safeName,
            arquivoPath = relative,
            mimeType = file.contentType,
            fileSize = file.size
        )
        documentRepository.save(doc)
        bidding.editalUrl = "/api/biddings/files/download?path=${java.net.URLEncoder.encode(relative, Charsets.UTF_8)}"
        biddingRepository.save(bidding)

        return mapOf(
            "url" to bidding.editalUrl!!,
            "path" to relative,
            "documentId" to doc.id!!,
            "fileName" to safeName
        )
    }

    fun resolveFile(relativePath: String): Path? {
        val base = Paths.get(uploadDir).toAbsolutePath().normalize()
        // Rejeita caminhos com sequências de traversal antes de resolver
        if (relativePath.contains("..")) return null
        val requested = base.resolve(relativePath).normalize()
        // Previne path traversal: garante que o arquivo está dentro do diretório de uploads
        if (!requested.startsWith(base)) return null
        return if (Files.exists(requested) && Files.isRegularFile(requested)) requested else null
    }

    private fun ensureBidding(biddingId: UUID, tenantId: UUID): Bidding {
        val bidding = biddingRepository.findById(biddingId).orElse(null)
            ?: throw ResourceNotFoundException("Licitação não encontrada")
        if (bidding.tenantId != tenantId) throw IllegalArgumentException("Licitação não pertence ao tenant")
        return bidding
    }
}

@Service
class BiddingProposalService(
    private val repository: BiddingProposalRepository,
    private val biddingService: BiddingService
) {
    fun list(biddingId: UUID, tenantId: UUID) =
        repository.findByBiddingIdAndTenantIdOrderByVersaoDesc(biddingId, tenantId).map { BiddingProposalResponse.from(it) }

    @Transactional
    fun create(biddingId: UUID, tenantId: UUID, request: BiddingProposalRequest): BiddingProposalResponse {
        val bidding = biddingService.getEntityByIdAndTenant(biddingId, tenantId)
            ?: throw ResourceNotFoundException("Licitação não encontrada")
        val nextVersao = (repository.findByBiddingIdAndTenantIdOrderByVersaoDesc(biddingId, tenantId).firstOrNull()?.versao ?: 0) + 1
        val margem = computeMargem(request.valorProposta, request.custoTotal, request.margemEstimadaPct)
        val proposal = BiddingProposal(
            tenantId = tenantId,
            bidding = bidding,
            versao = nextVersao,
            cenario = request.cenario,
            valorProposta = request.valorProposta,
            margemEstimadaPct = margem,
            custoTotal = request.custoTotal,
            observacoes = request.observacoes,
            tributacaoRegime = request.tributacaoRegime
        )
        return BiddingProposalResponse.from(repository.save(proposal))
    }

    private fun computeMargem(
        valor: java.math.BigDecimal?,
        custo: java.math.BigDecimal?,
        explicit: java.math.BigDecimal?
    ): java.math.BigDecimal? {
        if (explicit != null) return explicit
        if (valor != null && custo != null && custo > java.math.BigDecimal.ZERO) {
            return valor.subtract(custo)
                .multiply(java.math.BigDecimal("100"))
                .divide(valor, 4, java.math.RoundingMode.HALF_UP)
        }
        return null
    }
}

@Service
class BiddingDeadlineService(
    private val repository: BiddingDeadlineRepository,
    private val biddingService: BiddingService
) {
    fun list(biddingId: UUID, tenantId: UUID) =
        repository.findByBiddingIdAndTenantIdOrderByDataLimiteAsc(biddingId, tenantId).map { BiddingDeadlineResponse.from(it) }

    @Transactional
    fun create(biddingId: UUID, tenantId: UUID, request: BiddingDeadlineRequest): BiddingDeadlineResponse {
        if (!biddingService.existsByIdAndTenant(biddingId, tenantId)) {
            throw ResourceNotFoundException("Licitação não encontrada")
        }
        val d = BiddingDeadline(
            tenantId = tenantId,
            biddingId = biddingId,
            tipo = request.tipo,
            descricao = request.descricao,
            dataLimite = request.dataLimite,
            alertaDiasAntes = request.alertaDiasAntes
        )
        return BiddingDeadlineResponse.from(repository.save(d))
    }

    @Transactional
    fun markDone(id: UUID, tenantId: UUID): BiddingDeadlineResponse {
        val d = repository.findById(id).orElseThrow { ResourceNotFoundException("Prazo não encontrado") }
        if (d.tenantId != tenantId) throw IllegalArgumentException("Prazo não pertence ao tenant")
        d.concluido = true
        return BiddingDeadlineResponse.from(repository.save(d))
    }
}

@Service
class BiddingItemService(
    private val repository: BiddingItemRepository,
    private val lotRepository: com.contractops.api.bidding.repository.BiddingLotRepository
) {
    fun listByLot(lotId: UUID, tenantId: UUID): List<BiddingItemResponse> =
        repository.findByBiddingLotIdAndTenantId(lotId, tenantId).map { BiddingItemResponse.from(it) }

    @Transactional
    fun create(lotId: UUID, tenantId: UUID, request: BiddingItemRequest): BiddingItemResponse {
        val lot = lotRepository.findById(lotId).orElseThrow { ResourceNotFoundException("Lote não encontrado") }
        if (lot.tenantId != tenantId) throw IllegalArgumentException("Lote não pertence ao tenant")
        val total = request.valorUnitario?.multiply(request.quantidade)
        val item = BiddingItem(
            tenantId = tenantId,
            biddingLot = lot,
            codigoItem = request.codigoItem,
            descricao = request.descricao,
            unidade = request.unidade,
            quantidade = request.quantidade,
            valorUnitario = request.valorUnitario,
            valorTotal = total,
            tipo = request.tipo
        )
        return BiddingItemResponse.from(repository.save(item))
    }
}

@Service
class BiddingImpugnacaoService(
    private val repository: BiddingImpugnacaoRepository,
    private val biddingService: BiddingService
) {
    fun list(biddingId: UUID, tenantId: UUID) = repository.findByBiddingIdAndTenantId(biddingId, tenantId)

    @Transactional
    fun create(biddingId: UUID, tenantId: UUID, request: BiddingImpugnacaoRequest): BiddingImpugnacao {
        if (!biddingService.existsByIdAndTenant(biddingId, tenantId)) {
            throw ResourceNotFoundException("Licitação não encontrada")
        }
        return repository.save(
            BiddingImpugnacao(
                tenantId = tenantId,
                biddingId = biddingId,
                tipo = request.tipo,
                protocolo = request.protocolo,
                dataProtocolo = request.dataProtocolo,
                argumentos = request.argumentos
            )
        )
    }
}

@Service
class BiddingAtaService(
    private val repository: BiddingAtaRepository,
    private val biddingService: BiddingService,
    private val biddingRepository: com.contractops.api.bidding.repository.BiddingRepository
) {
    fun list(biddingId: UUID, tenantId: UUID) = repository.findByBiddingIdAndTenantId(biddingId, tenantId)

    @Transactional
    fun create(biddingId: UUID, tenantId: UUID, request: BiddingAtaRequest): BiddingAta {
        val bidding = biddingService.getEntityByIdAndTenant(biddingId, tenantId)
            ?: throw ResourceNotFoundException("Licitação não encontrada")
        request.numeroAta?.let { bidding.numeroAta = it }
        biddingRepository.save(bidding)
        return repository.save(
            BiddingAta(
                tenantId = tenantId,
                biddingId = biddingId,
                numeroAta = request.numeroAta,
                dataSessao = request.dataSessao,
                resumo = request.resumo
            )
        )
    }
}
