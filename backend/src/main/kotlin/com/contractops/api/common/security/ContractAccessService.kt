package com.contractops.api.common.security

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.contract.repository.ContractRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.util.*

/**
 * ABAC stub — verifica acesso a contratos por tenant + role (SPEC §2.1 / §24).
 */
@Service("contractAccessService")
class ContractAccessService(
    private val contractRepository: ContractRepository
) {
    private val fullTenantRoles = setOf(
        AppRole.ADMIN.authority,
        AppRole.GESTOR_GRUPO.authority,
        AppRole.FINANCEIRO.authority,
        AppRole.CONTADOR.authority,
        AppRole.DP.authority,
        AppRole.FISCAL_INTERNO.authority
    )

    private val contractScopedRoles = setOf(
        AppRole.GESTOR_CONTRATO.authority,
        AppRole.SUPERVISOR.authority,
        AppRole.PORTAL_ORGAO.authority
    )

    fun canAccessContract(authentication: Authentication, contractId: UUID): Boolean {
        val tenantId = TenantContext.getCurrentTenantId() ?: return false
        val contract = contractRepository.findById(contractId).orElse(null) ?: return false
        if (contract.tenantId != tenantId) return false

        val roles = authentication.authorities.map { it.authority }.toSet()
        if (roles.any { it in fullTenantRoles }) return true
        if (roles.any { it in contractScopedRoles }) return true
        return false
    }

    fun canWriteContract(authentication: Authentication, contractId: UUID): Boolean {
        val tenantId = TenantContext.getCurrentTenantId() ?: return false
        val contract = contractRepository.findById(contractId).orElse(null) ?: return false
        if (contract.tenantId != tenantId) return false

        val roles = authentication.authorities.map { it.authority }.toSet()
        if (roles.contains(AppRole.PORTAL_ORGAO.authority)) return false
        if (roles.any { it in fullTenantRoles }) return true
        return roles.any {
            it == AppRole.GESTOR_CONTRATO.authority || it == AppRole.SUPERVISOR.authority
        }
    }
}
