package com.contractops.api.bidding.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Entidade de Licitação (Bidding)
 * Alinhada com a SPEC v1.0 seções 5 e 25.
 * Parte do fluxo: Licitação → Lotes → Planilha Vencedora → Contrato (SPEC §5 + §6).
 */
@Entity
@Table(name = "biddings")
class Bidding(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "processo_numero", length = 100)
    var processoNumero: String? = null,

    @Column(name = "edital_numero", length = 100)
    var editalNumero: String? = null,

    @Column(name = "modalidade", length = 100)
    var modalidade: String? = null,

    @Column(name = "portal_origem", length = 100)
    var portalOrigem: String? = null,

    @Column(name = "orgao", length = 255)
    var orgao: String,

    @Column(name = "cnpj_orgao", length = 18)
    var cnpjOrgao: String? = null,

    @Column(name = "objeto", columnDefinition = "TEXT")
    var objeto: String? = null,

    @Column(name = "data_publicacao")
    var dataPublicacao: LocalDate? = null,

    @Column(name = "data_sessao")
    var dataSessao: LocalDate? = null,

    @Column(name = "data_homologacao")
    var dataHomologacao: LocalDate? = null,

    @Column(name = "data_adjudicacao")
    var dataAdjudicacao: LocalDate? = null,

    @Column(name = "valor_estimado", precision = 16, scale = 2)
    var valorEstimado: BigDecimal? = null,

    @Column(name = "valor_vencedor", precision = 16, scale = 2)
    var valorVencedor: BigDecimal? = null,

    @Column(name = "status", length = 50)
    var status: String = "HOMOLOGADA",

    @Column(name = "fonte_recurso", length = 100)
    var fonteRecurso: String? = null,

    @Column(name = "edital_url", length = 500)
    var editalUrl: String? = null,

    @Column(name = "vencedor_empresa", length = 255)
    var vencedorEmpresa: String? = null,

    @Column(name = "unidade_compradora", length = 255)
    var unidadeCompradora: String? = null,

    @Column(name = "regime_legal", length = 80)
    var regimeLegal: String? = "LEI_14133",

    @Column(name = "equipe_responsavel", length = 255)
    var equipeResponsavel: String? = null,

    @Column(name = "garantia_proposta", precision = 16, scale = 2)
    var garantiaProposta: BigDecimal? = null,

    @Column(name = "riscos_identificados", columnDefinition = "TEXT")
    var riscosIdentificados: String? = null,

    @Column(name = "links_externos", columnDefinition = "TEXT")
    var linksExternos: String? = null,

    @Column(name = "pncp_id", length = 120)
    var pncpId: String? = null,

    @Column(name = "numero_ata", length = 100)
    var numeroAta: String? = null,

    @Column(name = "numero_contrato_ref", length = 100)
    var numeroContratoRef: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "Bidding(id=$id, edital='$editalNumero', orgao='$orgao')"
}
