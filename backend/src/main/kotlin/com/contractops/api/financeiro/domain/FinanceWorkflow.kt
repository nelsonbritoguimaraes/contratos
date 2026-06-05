package com.contractops.api.financeiro.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "finance_workflows")
class FinanceWorkflow(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "tipo", length = 50, nullable = false)
    var tipo: String = "NFS_COBRANCA_CONCILIACAO",

    @Column(name = "nota_fiscal_id")
    var notaFiscalId: UUID? = null,

    @Column(name = "conta_a_receber_id")
    var contaAReceberId: UUID? = null,

    @Column(name = "estado_atual", length = 50, nullable = false)
    var estadoAtual: String,

    @Column(name = "historico_json", columnDefinition = "TEXT")
    var historicoJson: String? = null,

    @Column(name = "erro", columnDefinition = "TEXT")
    var erro: String? = null,

    @Column(name = "concluido", nullable = false)
    var concluido: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
