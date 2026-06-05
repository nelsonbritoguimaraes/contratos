package com.contractops.api.bidding.api

import com.contractops.api.bidding.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

// --- Postos ---
data class BiddingPostoRequest(
    val biddingLotId: UUID? = null,
    val codigo: String? = null,
    val nome: String,
    val funcao: String? = null,
    val cbo: String? = null,
    val escala: String? = null,
    val jornadaHoras: Int? = null,
    val valorMensal: BigDecimal? = null,
    val localExecucao: String? = null,
    val municipioExecucao: String? = null,
    val quantidade: Int = 1
)

data class BiddingPostoResponse(
    val id: UUID?,
    val biddingId: UUID,
    val biddingLotId: UUID?,
    val codigo: String?,
    val nome: String,
    val funcao: String?,
    val cbo: String?,
    val escala: String?,
    val jornadaHoras: Int?,
    val valorMensal: BigDecimal?,
    val localExecucao: String?,
    val municipioExecucao: String?,
    val quantidade: Int
) {
    companion object {
        fun from(e: BiddingPosto) = BiddingPostoResponse(
            id = e.id,
            biddingId = e.bidding.id!!,
            biddingLotId = e.biddingLotId,
            codigo = e.codigo,
            nome = e.nome,
            funcao = e.funcao,
            cbo = e.cbo,
            escala = e.escala,
            jornadaHoras = e.jornadaHoras,
            valorMensal = e.valorMensal,
            localExecucao = e.localExecucao,
            municipioExecucao = e.municipioExecucao,
            quantidade = e.quantidade
        )
    }
}

// --- Itens ---
data class BiddingItemRequest(
    val codigoItem: String? = null,
    val descricao: String,
    val unidade: String? = null,
    val quantidade: BigDecimal = BigDecimal.ONE,
    val valorUnitario: BigDecimal? = null,
    val tipo: String = "SERVICO"
)

data class BiddingItemResponse(
    val id: UUID?,
    val biddingLotId: UUID,
    val codigoItem: String?,
    val descricao: String,
    val unidade: String?,
    val quantidade: BigDecimal,
    val valorUnitario: BigDecimal?,
    val valorTotal: BigDecimal?,
    val tipo: String
) {
    companion object {
        fun from(e: BiddingItem) = BiddingItemResponse(
            id = e.id,
            biddingLotId = e.biddingLot.id!!,
            codigoItem = e.codigoItem,
            descricao = e.descricao,
            unidade = e.unidade,
            quantidade = e.quantidade,
            valorUnitario = e.valorUnitario,
            valorTotal = e.valorTotal,
            tipo = e.tipo
        )
    }
}

// --- Propostas ---
data class BiddingProposalRequest(
    val cenario: String = "BASE",
    val valorProposta: BigDecimal? = null,
    val margemEstimadaPct: BigDecimal? = null,
    val custoTotal: BigDecimal? = null,
    val observacoes: String? = null,
    val tributacaoRegime: String? = null
)

data class BiddingProposalResponse(
    val id: UUID?,
    val biddingId: UUID,
    val versao: Int,
    val cenario: String,
    val status: String,
    val valorProposta: BigDecimal?,
    val margemEstimadaPct: BigDecimal?,
    val custoTotal: BigDecimal?,
    val observacoes: String?,
    val tributacaoRegime: String?
) {
    companion object {
        fun from(e: BiddingProposal) = BiddingProposalResponse(
            id = e.id,
            biddingId = e.bidding.id!!,
            versao = e.versao,
            cenario = e.cenario,
            status = e.status,
            valorProposta = e.valorProposta,
            margemEstimadaPct = e.margemEstimadaPct,
            custoTotal = e.custoTotal,
            observacoes = e.observacoes,
            tributacaoRegime = e.tributacaoRegime
        )
    }
}

// --- Prazos ---
data class BiddingDeadlineRequest(
    val tipo: String,
    val descricao: String? = null,
    val dataLimite: OffsetDateTime,
    val alertaDiasAntes: Int = 3
)

data class BiddingDeadlineResponse(
    val id: UUID?,
    val biddingId: UUID,
    val tipo: String,
    val descricao: String?,
    val dataLimite: OffsetDateTime,
    val alertaDiasAntes: Int,
    val concluido: Boolean
) {
    companion object {
        fun from(e: BiddingDeadline) = BiddingDeadlineResponse(
            id = e.id,
            biddingId = e.biddingId,
            tipo = e.tipo,
            descricao = e.descricao,
            dataLimite = e.dataLimite,
            alertaDiasAntes = e.alertaDiasAntes,
            concluido = e.concluido
        )
    }
}

// --- Documentos ---
data class BiddingDocumentResponse(
    val id: UUID?,
    val biddingId: UUID,
    val tipo: String,
    val titulo: String,
    val arquivoNome: String?,
    val arquivoPath: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val url: String?
) {
    companion object {
        fun from(e: BiddingDocument) = BiddingDocumentResponse(
            id = e.id,
            biddingId = e.biddingId,
            tipo = e.tipo,
            titulo = e.titulo,
            arquivoNome = e.arquivoNome,
            arquivoPath = e.arquivoPath,
            mimeType = e.mimeType,
            fileSize = e.fileSize,
            url = e.arquivoPath?.let { "/api/biddings/files/download?path=${java.net.URLEncoder.encode(it, Charsets.UTF_8)}" }
        )
    }
}

// --- Impugnações / Atas ---
data class BiddingImpugnacaoRequest(
    val tipo: String,
    val protocolo: String? = null,
    val dataProtocolo: LocalDate? = null,
    val argumentos: String? = null
)

data class BiddingAtaRequest(
    val numeroAta: String? = null,
    val dataSessao: OffsetDateTime? = null,
    val resumo: String? = null
)

// --- Workflow ---
data class BiddingStatusTransitionRequest(val novoStatus: String)

// --- PNCP ---
data class PncpSearchRequest(val termo: String? = null, val cnpjOrgao: String? = null, val pagina: Int = 1)

data class BiddingExtendedFieldsRequest(
    val editalUrl: String? = null,
    val vencedorEmpresa: String? = null,
    val unidadeCompradora: String? = null,
    val regimeLegal: String? = null,
    val equipeResponsavel: String? = null,
    val garantiaProposta: BigDecimal? = null,
    val riscosIdentificados: String? = null,
    val linksExternos: String? = null,
    val pncpId: String? = null,
    val numeroAta: String? = null
)
