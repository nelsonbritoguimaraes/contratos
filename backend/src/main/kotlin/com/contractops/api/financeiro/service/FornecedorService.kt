package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.ContaAPagar
import com.contractops.api.financeiro.domain.Fornecedor
import com.contractops.api.financeiro.domain.LancamentoFinanceiro
import com.contractops.api.financeiro.repository.ContaAPagarRepository
import com.contractops.api.financeiro.repository.FornecedorRepository
import com.contractops.api.financeiro.repository.LancamentoFinanceiroRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class FornecedorService(
    private val fornecedorRepository: FornecedorRepository,
    private val lancamentoRepository: LancamentoFinanceiroRepository,
    private val contaAPagarRepository: ContaAPagarRepository
) {
    fun listar(tenantId: UUID, apenasAtivos: Boolean = true): List<Fornecedor> =
        if (apenasAtivos) fornecedorRepository.findByTenantIdAndAtivoTrueOrderByRazaoSocialAsc(tenantId)
        else fornecedorRepository.findByTenantIdOrderByRazaoSocialAsc(tenantId)

    @Transactional
    fun criar(fornecedor: Fornecedor): Fornecedor = fornecedorRepository.save(fornecedor)

    @Transactional
    fun atualizar(id: UUID, tenantId: UUID, updater: (Fornecedor) -> Unit): Fornecedor {
        val f = fornecedorRepository.findById(id).orElseThrow { IllegalArgumentException("Fornecedor não encontrado") }
        require(f.tenantId == tenantId)
        updater(f)
        return fornecedorRepository.save(f)
    }

    fun listarLancamentos(tenantId: UUID, tipo: String?): List<LancamentoFinanceiro> =
        if (tipo != null) lancamentoRepository.findByTenantIdAndTipoOrderByDataLancamentoDesc(tenantId, tipo)
        else lancamentoRepository.findByTenantIdOrderByDataLancamentoDesc(tenantId)

    @Transactional
    fun criarLancamento(lancamento: LancamentoFinanceiro): LancamentoFinanceiro {
        val saved = lancamentoRepository.save(lancamento)
        val tipo = lancamento.tipo.uppercase()
        if (tipo in listOf("DESPESA", "COMPRA")) {
            val ap = ContaAPagar(
                tenantId = lancamento.tenantId,
                origem = "FORNECEDOR",
                origemId = saved.id,
                valor = lancamento.valor,
                vencimento = lancamento.dataLancamento.plusDays(30),
                status = "ABERTO",
                observacoes = lancamento.descricao
            )
            val apSaved = contaAPagarRepository.save(ap)
            saved.contaAPagarId = apSaved.id
            saved.status = "AP_GERADA"
            return lancamentoRepository.save(saved)
        }
        return saved
    }
}
