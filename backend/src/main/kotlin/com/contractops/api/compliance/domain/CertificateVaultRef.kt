package com.contractops.api.compliance.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "certificate_vault_refs")
class CertificateVaultRef(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "company_id")
    val companyId: UUID? = null,

    @Column(name = "cnpj", nullable = false, length = 18)
    var cnpj: String,

    @Column(name = "alias", nullable = false, length = 100)
    var alias: String,

    @Column(name = "vault_path", nullable = false, length = 500)
    var vaultPath: String,

    @Column(name = "cert_type", nullable = false, length = 10)
    var certType: String = "A1",

    @Column(name = "expires_at")
    var expiresAt: LocalDate? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true
) : AuditEntity(), TenantAware
