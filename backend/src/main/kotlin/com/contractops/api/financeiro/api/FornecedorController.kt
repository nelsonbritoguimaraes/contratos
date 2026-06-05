package com.contractops.api.financeiro.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.financeiro.domain.Fornecedor
import com.contractops.api.financeiro.domain.LancamentoFinanceiro
import com.contractops.api.financeiro.service.FornecedorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/financeiro")
class FornecedorController(
    private val fornecedorService: FornecedorService
) {
    @GetMapping("/fornecedores")
    fun listar(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "true") apenasAtivos: Boolean
    ): ResponseEntity<List<FornecedorResponse>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val list = fornecedorService.listar(t, apenasAtivos).map { FornecedorResponse.from(it) }
        return ResponseEntity.ok(list)
    }

    @PostMapping("/fornecedores")
    fun criar(
        @RequestBody req: FornecedorRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<FornecedorResponse> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val f = Fornecedor(
            tenantId = t,
            razaoSocial = req.razaoSocial.trim(),
            cnpj = req.cnpj?.trim(),
            contato = req.contato,
            categoria = req.categoria
        )
        return ResponseEntity.ok(FornecedorResponse.from(fornecedorService.criar(f)))
    }

    @GetMapping("/lancamentos")
    fun listarLancamentos(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(required = false) tipo: String?
    ): ResponseEntity<List<LancamentoResponse>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(
            fornecedorService.listarLancamentos(t, tipo).map { LancamentoResponse.from(it) }
        )
    }

    @PostMapping("/lancamentos")
    fun criarLancamento(
        @RequestBody req: LancamentoRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<LancamentoResponse> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val l = LancamentoFinanceiro(
            tenantId = t,
            tipo = req.tipo.uppercase(),
            fornecedorId = req.fornecedorId,
            descricao = req.descricao,
            valor = req.valor,
            dataLancamento = req.data ?: LocalDate.now(),
            categoria = req.categoria,
            status = req.status ?: "PENDENTE"
        )
        return ResponseEntity.ok(LancamentoResponse.from(fornecedorService.criarLancamento(l)))
    }
}

data class FornecedorRequest(
    val razaoSocial: String,
    val cnpj: String? = null,
    val contato: String? = null,
    val categoria: String? = null
)

data class FornecedorResponse(
    val id: UUID,
    val razaoSocial: String,
    val cnpj: String?,
    val contato: String?,
    val categoria: String?,
    val ativo: Boolean
) {
    companion object {
        fun from(f: Fornecedor) = FornecedorResponse(
            id = f.id!!,
            razaoSocial = f.razaoSocial,
            cnpj = f.cnpj,
            contato = f.contato,
            categoria = f.categoria,
            ativo = f.ativo
        )
    }
}

data class LancamentoRequest(
    val tipo: String,
    val fornecedorId: UUID? = null,
    val descricao: String,
    val valor: BigDecimal,
    val data: LocalDate? = null,
    val categoria: String? = null,
    val status: String? = null
)

data class LancamentoResponse(
    val id: UUID,
    val tipo: String,
    val fornecedorId: UUID?,
    val descricao: String,
    val valor: BigDecimal,
    val data: LocalDate,
    val categoria: String?,
    val status: String
) {
    companion object {
        fun from(l: LancamentoFinanceiro) = LancamentoResponse(
            id = l.id!!,
            tipo = l.tipo,
            fornecedorId = l.fornecedorId,
            descricao = l.descricao,
            valor = l.valor,
            data = l.dataLancamento,
            categoria = l.categoria,
            status = l.status
        )
    }
}
