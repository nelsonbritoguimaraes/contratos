package com.contractops.api.contabilidade.repository

import com.contractops.api.contabilidade.domain.AccountingRule
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AccountingRuleRepository : JpaRepository<AccountingRule, UUID> {
    fun findByTenantIdAndOrigemTipoAndAtivaTrue(tenantId: UUID, origemTipo: String): List<AccountingRule>
    fun findByTenantIdAndCodigo(tenantId: UUID, codigo: String): AccountingRule?
    fun findByTenantIdOrderByOrigemTipoAscCodigoAsc(tenantId: UUID): List<AccountingRule>
    fun findByTenantIdAndRubricCodeAndAtivaTrue(tenantId: UUID, rubricCode: String): AccountingRule?
}
