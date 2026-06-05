package com.contractops.api.financeiro.api

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

// ==================== TESOURARIA ====================

data class CriarContaBancariaRequest(
    val bancoCodigo: String,
    val bancoNome: String,
    val agencia: String,
    val conta: String,
    val tipo: String = "CORRENTE",
    val contaContabilId: UUID? = null,
    val observacoes: String? = null
)

data class ContaBancariaResponse(
    val id: UUID?,
    val bancoCodigo: String,
    val bancoNome: String,
    val agencia: String,
    val conta: String,
    val tipo: String,
    val saldoAtual: BigDecimal,
    val ativa: Boolean
)

// ==================== CONTAS A RECEBER ====================

data class CriarContaAReceberRequest(
    val contratoId: UUID?,
    val measurementId: UUID?,
    val valorBruto: BigDecimal,
    val valorLiquido: BigDecimal,
    val vencimento: LocalDate
)

// ==================== NFS-e ====================

data class EmitirNfsRequest(
    val measurementId: UUID,
    val contratoId: UUID,
    val tomadorCnpj: String,
    val valorServicos: BigDecimal
)

// ==================== FLUXO DE CAIXA ====================

data class FluxoCaixaRequest(
    val inicio: String,
    val fim: String
)

// ==================== CFO DASHBOARD ====================

data class CfoDashboardResponse(
    val dataCorte: String,
    val posicaoCaixa: Map<String, Any>,
    val kpis: Map<String, Any>,
    val alertas: List<String>
)

// ==================== CONTAS A RECEBER RICH (Fase 2/3) ====================

data class ContaAReceberResponse(
    val id: UUID?,
    val contratoId: UUID?,
    val measurementId: UUID?,
    val notaFiscalId: UUID?,
    val valorBruto: BigDecimal,
    val valorLiquido: BigDecimal,
    val vencimento: LocalDate,
    val status: String,
    val diasAtraso: Int,
    val jurosMulta: BigDecimal,
    val dataRecebimento: LocalDate?,
    val valorRecebido: BigDecimal?,
    val agingBucket: String,           // 0-30, 31-60, 61-90, 90+
    val saldoAberto: BigDecimal
)

data class ContasAReceberResumoResponse(
    val total: Int,
    val valorTotalAberto: BigDecimal,
    val valorTotalVencido: BigDecimal,
    val porStatus: Map<String, Int>,
    val porAging: Map<String, BigDecimal>,
    val contas: List<ContaAReceberResponse>
)

data class ContaAPagarResponse(
    val id: UUID?,
    val origem: String,
    val origemId: UUID?,
    val contratoId: UUID?,
    val valor: BigDecimal,
    val vencimento: LocalDate,
    val status: String,
    val dataPagamento: LocalDate?,
    val valorPago: BigDecimal?,
    val formaPagamento: String?,
    val observacoes: String?,
    val diasAtraso: Int,
    val agingBucket: String,
    val saldoAberto: BigDecimal
)

data class ContasAPagarResumoResponse(
    val total: Int,
    val valorTotalAberto: BigDecimal,
    val valorTotalVencido: BigDecimal,
    val porStatus: Map<String, Int>,
    val porOrigem: Map<String, Int>,
    val porAging: Map<String, BigDecimal>,
    val contas: List<ContaAPagarResponse>
)

data class RegistrarRecebimentoRequest(
    val valor: BigDecimal,
    val data: String,
    val contaBancariaId: UUID? = null,
    val observacao: String? = null
)

data class CalcularRetencoesRequest(
    val valorServico: BigDecimal,
    val municipioIbge: String = "3550308",
    val naturezaServico: String = "TERCEIRIZACAO",
    val aplicarInss: Boolean = true
)

data class RetencaoCalculadaResponse(
    val tipo: String,
    val aliquota: BigDecimal,
    val baseCalculo: BigDecimal,
    val valorRetido: BigDecimal,
    val codigoReceita: String?,
    val dataVencimento: String,
    val observacao: String?
)

data class DarfPreviewResponse(
    val retencaoId: UUID,
    val tipo: String,
    val codigoReceita: String?,
    val valor: BigDecimal,
    val competencia: String,
    val darfTexto: String,
    val avisoReforma2026: String
)

// ==================== IMPORTAÇÃO DE EXTRATO (Fase 3) ====================

data class ExtratoItemRequest(
    val data: LocalDate,
    val documento: String? = null,
    val historico: String,
    val valor: BigDecimal,
    val tipo: String   // CREDITO ou DEBITO
)

data class BaixarContaAPagarRequest(
    val data: String,
    val valor: BigDecimal,
    val contaBancariaId: UUID,
    val formaPagamento: String = "PIX"
)

data class GerarCobrancaRequest(
    val tipo: String = "PIX"
)

data class ProvisaoGlosaRequest(
    val measurementId: UUID,
    val valorGlosa: BigDecimal
)

data class TenantFiscalProfileResponse(
    val tenantId: UUID,
    val desoneracaoFolha: Boolean,
    val aliquotaInssRetencao: BigDecimal,
    val simplesNacional: Boolean,
    val municipioIbgePadrao: String?,
    val cnpjPrestador: String?,
    val updatedAt: String
)

data class UpdateTenantFiscalProfileRequest(
    val desoneracaoFolha: Boolean? = null,
    val aliquotaInssRetencao: BigDecimal? = null,
    val simplesNacional: Boolean? = null,
    val municipioIbgePadrao: String? = null,
    val cnpjPrestador: String? = null
)

data class FechamentoFinanceiroResponse(
    val id: UUID?,
    val dataInicio: LocalDate,
    val dataFim: LocalDate,
    val status: String,
    val saldoCaixaFinal: BigDecimal?,
    val totalRecebimentos: BigDecimal?,
    val totalPagamentos: BigDecimal?,
    val observacoes: String?,
    val dataFechamento: String?
)

data class IniciarOpenFinanceConsentRequest(
    val contaBancariaId: UUID? = null,
    val institutionId: String,
    val institutionName: String
)