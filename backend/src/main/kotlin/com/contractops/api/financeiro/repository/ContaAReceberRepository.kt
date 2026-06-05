package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.ContaAReceber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface ContaAReceberRepository : JpaRepository<ContaAReceber, UUID> {

    fun findByTenantId(tenantId: UUID): List<ContaAReceber>

    fun findByTenantIdAndContratoId(tenantId: UUID, contratoId: UUID): List<ContaAReceber>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<ContaAReceber>

    @Query("""
        SELECT c FROM ContaAReceber c 
        WHERE c.tenantId = :tenantId 
        AND c.status IN ('ABERTO', 'PARCIAL') 
        AND c.vencimento <= :ate
    """)
    fun findVencidasOuAVencer(tenantId: UUID, ate: LocalDate): List<ContaAReceber>

    fun findByTenantIdAndMeasurementId(tenantId: UUID, measurementId: UUID): List<ContaAReceber>

    fun findByTenantIdAndStatusIn(tenantId: UUID, status: Collection<String>): List<ContaAReceber>

    @Query("""
        SELECT c FROM ContaAReceber c 
        WHERE c.tenantId = :tenantId 
        AND (:contratoId IS NULL OR c.contratoId = :contratoId)
        AND (:status IS NULL OR c.status = :status)
        AND (:vencimentoDe IS NULL OR c.vencimento >= :vencimentoDe)
        AND (:vencimentoAte IS NULL OR c.vencimento <= :vencimentoAte)
        ORDER BY c.vencimento ASC
    """)
    fun findRich(
        tenantId: UUID,
        @Param("contratoId") contratoId: UUID?,
        @Param("status") status: String?,
        @Param("vencimentoDe") vencimentoDe: LocalDate?,
        @Param("vencimentoAte") vencimentoAte: LocalDate?
    ): List<ContaAReceber>
}