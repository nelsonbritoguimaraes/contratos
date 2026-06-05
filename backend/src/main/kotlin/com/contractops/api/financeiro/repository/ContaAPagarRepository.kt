package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.ContaAPagar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface ContaAPagarRepository : JpaRepository<ContaAPagar, UUID> {

    fun findByTenantId(tenantId: UUID): List<ContaAPagar>

    fun findByTenantIdAndOrigemAndOrigemId(tenantId: UUID, origem: String, origemId: UUID): List<ContaAPagar>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<ContaAPagar>

    @Query("""
        SELECT c FROM ContaAPagar c 
        WHERE c.tenantId = :tenantId 
        AND c.status IN ('ABERTO', 'APROVADO') 
        AND c.vencimento <= :ate
    """)
    fun findVencidasOuAVencer(tenantId: UUID, ate: LocalDate): List<ContaAPagar>

    fun findByTenantIdAndContratoId(tenantId: UUID, contratoId: UUID): List<ContaAPagar>

    @Query("""
        SELECT c FROM ContaAPagar c
        WHERE c.tenantId = :tenantId
        AND (:contratoId IS NULL OR c.contratoId = :contratoId)
        AND (:status IS NULL OR c.status = :status)
        AND (:origem IS NULL OR c.origem = :origem)
        AND (:vencimentoDe IS NULL OR c.vencimento >= :vencimentoDe)
        AND (:vencimentoAte IS NULL OR c.vencimento <= :vencimentoAte)
        ORDER BY c.vencimento ASC
    """)
    fun findRich(
        tenantId: UUID,
        @Param("contratoId") contratoId: UUID?,
        @Param("status") status: String?,
        @Param("origem") origem: String?,
        @Param("vencimentoDe") vencimentoDe: LocalDate?,
        @Param("vencimentoAte") vencimentoAte: LocalDate?
    ): List<ContaAPagar>
}