package com.contractops.api.contract.domain

import com.contractops.api.bidding.domain.Bidding
import com.contractops.api.bidding.domain.WinningSpreadsheet
import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Contrato (expandido)
 * Alinhado com SPEC v1.0 seções 6 e 25.
 */
@Entity
@Table(name = "contracts")
class Contract(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "company_id", nullable = false)
    val companyId: UUID,

    @Column(name = "branch_id")
    val branchId: UUID? = null,

    // Vinculação opcional com a licitação de origem
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidding_id")
    var bidding: Bidding? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_spreadsheet_id")
    var winningSpreadsheet: WinningSpreadsheet? = null,

    @Column(name = "numero", nullable = false, length = 50)
    var numero: String,

    @Column(name = "orgao", nullable = false, length = 255)
    var orgao: String,

    @Column(name = "cnpj_orgao", length = 18)
    var cnpjOrgao: String? = null,

    @Column(name = "objeto", columnDefinition = "TEXT")
    var objeto: String? = null,

    @Column(name = "vigencia_inicio")
    var vigenciaInicio: LocalDate? = null,

    @Column(name = "vigencia_fim")
    var vigenciaFim: LocalDate? = null,

    @Column(name = "valor_mensal", precision = 14, scale = 2)
    var valorMensal: BigDecimal? = null,

    @Column(name = "valor_global", precision = 16, scale = 2)
    var valorGlobal: BigDecimal? = null,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "ATIVO",

    @Column(name = "qtd_postos_contratados", nullable = false)
    var qtdPostosContratados: Int = 0,

    // Campos adicionais úteis (da SPEC)
    @Column(name = "preposto_nome", length = 150)
    var prepostoNome: String? = null,

    @Column(name = "gestor_orgao", length = 150)
    var gestorOrgao: String? = null,

    @Column(name = "fiscal_tecnico", length = 150)
    var fiscalTecnico: String? = null,

    @Column(name = "fiscal_administrativo", length = 150)
    var fiscalAdministrativo: String? = null,

    // Regras e configurações operacionais (da SPEC)
    @Column(name = "regras_glosa", columnDefinition = "TEXT")
    var regrasGlosa: String? = null,

    @Column(name = "regras_substituicao", columnDefinition = "TEXT")
    var regrasSubstituicao: String? = null,

    @Column(name = "regras_uniforme", columnDefinition = "TEXT")
    var regrasUniforme: String? = null,

    @Column(name = "regras_equipamentos", columnDefinition = "TEXT")
    var regrasEquipamentos: String? = null,

    @Column(name = "regras_faturamento", columnDefinition = "TEXT")
    var regrasFaturamento: String? = null,

    @Column(name = "regras_ponto", columnDefinition = "TEXT")
    var regrasPonto: String? = null,

    @Column(name = "regras_medicao", columnDefinition = "TEXT")
    var regrasMedicao: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "Contract(id=$id, numero='$numero', orgao='$orgao', status='$status')"
}
