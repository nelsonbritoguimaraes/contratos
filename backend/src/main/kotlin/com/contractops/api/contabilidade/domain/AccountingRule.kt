package com.contractops.api.contabilidade.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "accounting_rules")
class AccountingRule(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "codigo", nullable = false, length = 50)
    var codigo: String,

    @Column(name = "descricao", length = 200)
    var descricao: String? = null,

    @Column(name = "origem_tipo", nullable = false, length = 50)
    var origemTipo: String,

    @Column(name = "conta_debito_codigo", nullable = false, length = 30)
    var contaDebitoCodigo: String,

    @Column(name = "conta_credito_codigo", nullable = false, length = 30)
    var contaCreditoCodigo: String,

    @Column(name = "historico_padrao", length = 255)
    var historicoPadrao: String? = null,

    @Column(name = "rubric_code", length = 50)
    var rubricCode: String? = null,

    @Column(name = "rubric_type", length = 20)
    var rubricType: String? = null,

    @Column(name = "ativa", nullable = false)
    var ativa: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
