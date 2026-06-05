package com.contractops.api.post.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Posto de Serviço (ServicePost)
 * Alinhado com SPEC v1.0 seções 7 e 25.
 * Núcleo do modelo de operação com dedicação exclusiva de mão de obra.
 */

@Entity
@Table(name = "service_posts")
class ServicePost(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "codigo", length = 50)
    var codigo: String? = null,

    @Column(name = "nome", nullable = false, length = 150)
    var nome: String,

    @Column(name = "funcao", length = 100)
    var funcao: String? = null,

    @Column(name = "cbo", length = 10)
    var cbo: String? = null,

    @Column(name = "escala", length = 20)
    var escala: String? = null,

    @Column(name = "jornada_horas")
    var jornadaHoras: Int? = null,

    @Column(name = "valor_mensal", precision = 12, scale = 2)
    var valorMensal: BigDecimal? = null,

    @Column(name = "valor_diario", precision = 10, scale = 2)
    var valorDiario: BigDecimal? = null,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "ATIVO",

    @Column(name = "titular_nome", length = 150)
    var titularNome: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "ServicePost(id=$id, nome='$nome', contractId=$contractId, status='$status')"
}
