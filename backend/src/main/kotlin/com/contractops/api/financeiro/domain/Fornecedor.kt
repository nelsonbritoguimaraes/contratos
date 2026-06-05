package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "fornecedores")
class Fornecedor(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "razao_social", length = 200, nullable = false)
    var razaoSocial: String,

    @Column(name = "cnpj", length = 20)
    var cnpj: String? = null,

    @Column(name = "contato", length = 120)
    var contato: String? = null,

    @Column(name = "categoria", length = 100)
    var categoria: String? = null,

    @Column(name = "ativo", nullable = false)
    var ativo: Boolean = true
) : AuditEntity(), TenantAware
