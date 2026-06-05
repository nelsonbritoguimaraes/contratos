package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "punch_adjustments")
class PunchAdjustment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    var employeeId: UUID,

    @Column(name = "contract_id")
    var contractId: UUID? = null,

    @Column(name = "post_id")
    var postId: UUID? = null,

    @Column(name = "date", nullable = false)
    var date: LocalDate,

    @Column(name = "tipo", nullable = false, length = 30)
    var tipo: String,

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    var motivo: String,

    @Column(name = "evidencia_url", columnDefinition = "TEXT")
    var evidenciaUrl: String? = null,

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "PENDENTE",

    @Column(name = "solicitado_por", length = 150)
    var solicitadoPor: String? = null,

    @Column(name = "aprovado_supervisor_por", length = 150)
    var aprovadoSupervisorPor: String? = null,

    @Column(name = "aprovado_dp_por", length = 150)
    var aprovadoDpPor: String? = null,

    @Column(name = "antes_json", columnDefinition = "TEXT")
    var antesJson: String? = null,

    @Column(name = "depois_json", columnDefinition = "TEXT")
    var depoisJson: String? = null,

    @Column(name = "impacto_folha", precision = 14, scale = 2)
    var impactoFolha: BigDecimal? = null,

    @Column(name = "impacto_glosa", precision = 14, scale = 2)
    var impactoGlosa: BigDecimal? = null
) : AuditEntity()
