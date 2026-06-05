package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Pagamento executado (baixa de Conta a Pagar).
 * SPEC §22.2
 */
@Entity
@Table(name = "pagamentos")
class Pagamento(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "conta_a_pagar_id", nullable = false)
    var contaAPagarId: UUID,

    @Column(name = "data", nullable = false)
    var data: LocalDate,

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "conta_bancaria_id")
    var contaBancariaId: UUID? = null,

    @Column(name = "forma_pagamento", length = 30)
    var formaPagamento: String? = null,

    @Column(name = "comprovante_url", length = 500)
    var comprovanteUrl: String? = null,

    @Column(name = "usuario_aprovador", length = 100)
    var usuarioAprovador: String? = null,

    @Column(name = "nivel_aprovacao", nullable = false)
    var nivelAprovacao: Int = 1

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "Pagamento(id=$id, contaAPagarId=$contaAPagarId, valor=$valor, data=$data)"
}