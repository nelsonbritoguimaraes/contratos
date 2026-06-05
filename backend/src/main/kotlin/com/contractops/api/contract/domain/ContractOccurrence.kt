package com.contractops.api.contract.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "contract_occurrences")
class ContractOccurrence(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "data_ocorrencia", nullable = false)
    var dataOcorrencia: LocalDate,

    @Column(name = "tipo", nullable = false, length = 50)
    var tipo: String,

    @Column(name = "titulo", nullable = false, length = 200)
    var titulo: String,

    @Column(name = "descricao", columnDefinition = "TEXT")
    var descricao: String? = null,

    @Column(name = "severidade", nullable = false, length = 20)
    var severidade: String = "INFO",

    @Column(name = "registrado_por", length = 150)
    var registradoPor: String? = null,

    @Column(name = "anexo_ref", length = 255)
    var anexoRef: String? = null
) : AuditEntity(), TenantAware
