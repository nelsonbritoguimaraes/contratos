package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.LancamentoFinanceiro
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface LancamentoFinanceiroRepository : JpaRepository<LancamentoFinanceiro, UUID> {
    fun findByTenantIdAndTipoOrderByDataLancamentoDesc(tenantId: UUID, tipo: String): List<LancamentoFinanceiro>
    fun findByTenantIdOrderByDataLancamentoDesc(tenantId: UUID): List<LancamentoFinanceiro>
}
