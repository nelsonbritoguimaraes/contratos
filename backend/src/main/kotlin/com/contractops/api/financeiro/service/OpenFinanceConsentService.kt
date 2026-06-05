package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.config.OpenFinanceProperties
import com.contractops.api.financeiro.domain.OpenFinanceConsent
import com.contractops.api.financeiro.exception.FinanceiroBusinessException
import com.contractops.api.financeiro.repository.OpenFinanceConsentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
class OpenFinanceConsentService(
    private val repository: OpenFinanceConsentRepository,
    private val auditService: FinancialAuditService,
    private val openFinanceProperties: OpenFinanceProperties
) {
    fun listar(tenantId: UUID): List<OpenFinanceConsent> =
        repository.findByTenantIdOrderByCreatedAtDesc(tenantId)

    @Transactional
    fun iniciarConsentimento(
        tenantId: UUID,
        contaBancariaId: UUID?,
        institutionId: String,
        institutionName: String
    ): OpenFinanceConsent {
        val consentId = "consent-${UUID.randomUUID()}"
        val authUrl = openFinanceProperties.buildAuthorizationUrl(consentId, institutionId, tenantId)
        val consent = OpenFinanceConsent(
            tenantId = tenantId,
            contaBancariaId = contaBancariaId,
            institutionId = institutionId,
            institutionName = institutionName,
            consentId = consentId,
            status = "PENDING",
            authorizationUrl = authUrl,
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val saved = repository.save(consent)
        auditService.registrar(tenantId, "OPEN_FINANCE", saved.id, "INICIAR_CONSENT", "sistema", institutionName)
        return saved
    }

    @Transactional
    fun confirmarConsentimento(consentRecordId: UUID, tenantId: UUID): OpenFinanceConsent {
        val consent = repository.findById(consentRecordId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Consentimento não encontrado") }
        consent.status = "AUTHORIZED"
        consent.updatedAt = OffsetDateTime.now()
        val saved = repository.save(consent)
        auditService.registrar(tenantId, "OPEN_FINANCE", saved.id, "AUTORIZAR", "sistema", "Consent ${saved.consentId}")
        return saved
    }

    @Transactional
    fun revogarConsentimento(consentRecordId: UUID, tenantId: UUID): OpenFinanceConsent {
        val consent = repository.findById(consentRecordId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Consentimento não encontrado") }
        consent.status = "REVOKED"
        consent.updatedAt = OffsetDateTime.now()
        return repository.save(consent)
    }
}
