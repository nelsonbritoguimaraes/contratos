package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "lancamentos_financeiros")
class LancamentoFinanceiro(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "tipo", length = 20, nullable = false)
    var tipo: String,

    @Column(name = "fornecedor_id")
    var fornecedorId: UUID? = null,

    @Column(name = "descricao", columnDefinition = "TEXT", nullable = false)
    var descricao: String,

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "data_lancamento", nullable = false)
    var dataLancamento: LocalDate,

    @Column(name = "categoria", length = 100)
    var categoria: String? = null,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "PENDENTE",

    @Column(name = "conta_a_pagar_id")
    var contaAPagarId: UUID? = null,

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null
) : AuditEntity(), TenantAware
