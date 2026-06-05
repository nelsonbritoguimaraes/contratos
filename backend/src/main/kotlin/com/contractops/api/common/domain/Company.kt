package com.contractops.api.common.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Empresa / Pessoa Jurídica (CNPJ)
 * Alinhado com SPEC v1.0 seções 4 e 25.1 (Núcleo organizacional).
 */
@Entity
@Table(name = "companies", uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "cnpj"])])
class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "cnpj", nullable = false, length = 18)
    var cnpj: String,

    @Column(name = "razao_social", nullable = false, length = 255)
    var razaoSocial: String,

    @Column(name = "nome_fantasia", length = 255)
    var nomeFantasia: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "Company(id=$id, cnpj='$cnpj', razaoSocial='$razaoSocial')"
}
