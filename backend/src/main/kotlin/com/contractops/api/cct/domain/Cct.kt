package com.contractops.api.cct.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

/**
 * Convenção Coletiva de Trabalho (CCT) / Acordo Coletivo.
 * Fundamental para regras salariais, benefícios, uniformes, etc.
 * SPEC §4.15 e §26.
 */
@Entity
@Table(name = "ccts")
class Cct(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id")
    val contractId: UUID? = null,

    @Column(name = "sindicato", length = 200)
    var sindicato: String? = null,

    @Column(name = "vigencia_inicio")
    var vigenciaInicio: LocalDate? = null,

    @Column(name = "vigencia_fim")
    var vigenciaFim: LocalDate? = null,

    @Column(name = "arquivo_nome", length = 255)
    var arquivoNome: String? = null,

    @Column(name = "raw_text", columnDefinition = "TEXT")
    var rawText: String? = null,

    @Column(name = "extracted_data", columnDefinition = "TEXT")
    var extractedData: String? = null,   // JSON simples ou texto estruturado com cláusulas chave

    @Column(name = "status", length = 30)
    var status: String = "ATIVO"

) : AuditEntity(), TenantAware {
    override fun toString(): String = "Cct(id=$id, sindicato='$sindicato')"
}