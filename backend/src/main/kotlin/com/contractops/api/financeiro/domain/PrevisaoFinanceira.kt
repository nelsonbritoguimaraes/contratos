package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Previsão de Fluxo de Caixa (projetado).
 * Motor de forecasting do CFO (13 semanas / 12 meses).
 * SPEC §22.3
 */
@Entity
@Table(name = "previsoes_financeiras")
class PrevisaoFinanceira(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "data", nullable = false)
    var data: LocalDate,

    @Column(name = "tipo", length = 50, nullable = false)
    var tipo: String,                         // RECEBIMENTO_PROJETADO, PAGAMENTO_FOLHA, TRIBUTO, FORNECEDOR, CAPEX, OUTROS

    @Column(name = "valor", precision = 16, scale = 2, nullable = false)
    var valor: BigDecimal,

    @Column(name = "contrato_id")
    val contratoId: UUID? = null,

    @Column(name = "probabilidade", nullable = false)
    var probabilidade: Int = 80,              // 0 a 100

    @Column(name = "cenario", length = 20, nullable = false)
    var cenario: String = "BASE",             // BASE, OTIMISTA, PESSIMISTA

    @Column(name = "descricao", columnDefinition = "TEXT")
    var descricao: String? = null,

    @Column(name = "origem", length = 50)
    var origem: String? = null                // MANUAL, SISTEMA, SIMULACAO

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "PrevisaoFinanceira(id=$id, data=$data, tipo='$tipo', valor=$valor, cenario='$cenario')"
}