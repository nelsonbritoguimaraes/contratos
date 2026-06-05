package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ExtratoBancarioItemRepository : JpaRepository<ExtratoBancarioItem, UUID> {

    fun findByTenantIdAndContaBancariaId(
        tenantId: UUID,
        contaBancariaId: UUID
    ): List<ExtratoBancarioItem>

    fun findByTenantIdAndContaBancariaIdAndConciliadoFalse(
        tenantId: UUID,
        contaBancariaId: UUID
    ): List<ExtratoBancarioItem>

    fun findByTenantIdAndConciliacaoId(tenantId: UUID, conciliacaoId: UUID): List<ExtratoBancarioItem>
}