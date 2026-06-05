package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.Fornecedor
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FornecedorRepository : JpaRepository<Fornecedor, UUID> {
    fun findByTenantIdAndAtivoTrueOrderByRazaoSocialAsc(tenantId: UUID): List<Fornecedor>
    fun findByTenantIdOrderByRazaoSocialAsc(tenantId: UUID): List<Fornecedor>
}
