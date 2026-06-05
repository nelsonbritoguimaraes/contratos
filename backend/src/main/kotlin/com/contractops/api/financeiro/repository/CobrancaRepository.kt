package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.Cobranca
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CobrancaRepository : JpaRepository<Cobranca, UUID> {
    fun findByTenantIdAndContaAReceberId(tenantId: UUID, contaAReceberId: UUID): List<Cobranca>
}
