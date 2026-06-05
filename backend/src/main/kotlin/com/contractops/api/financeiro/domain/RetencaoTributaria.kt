package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Retenção Tributária vinculada a NFS-e ou Conta a Pagar.
 * ISS, PIS, COFINS, CSLL, IRRF, INSS.
 * SPEC §21.2 + §22
 */
@Entity
@Table(name = "retencoes_tributarias")
class RetencaoTributaria(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "nota_fiscal_id")
    val notaFiscalId: UUID? = null,

    @Column(name = "conta_a_pagar_id")
    var contaAPagarId: UUID? = null,

    @Column(name = "tipo", length = 30, nullable = false)
    var tipo: String,                         // ISS, PIS, COFINS, CSLL, IRRF, INSS, OUTROS

    @Column(name = "aliquota", precision = 5, scale = 4, nullable = false)
    var aliquota: BigDecimal,

    @Column(name = "base_calculo", precision = 16, scale = 2, nullable = false)
    var baseCalculo: BigDecimal,

    @Column(name = "valor_retido", precision = 16, scale = 2, nullable = false)
    var valorRetido: BigDecimal,

    @Column(name = "codigo_receita", length = 20)
    var codigoReceita: String? = null,        // para DARF/GPS

    @Column(name = "data_vencimento")
    var dataVencimento: LocalDate? = null,

    @Column(name = "status", length = 20, nullable = false)
    var status: String = "PENDENTE",          // PENDENTE, PAGO, VENCIDO

    @Column(name = "darf_gerado", columnDefinition = "TEXT")
    var darfGerado: String? = null            // texto estruturado ou código de barras

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "RetencaoTributaria(id=$id, tipo='$tipo', valorRetido=$valorRetido, status='$status')"
}