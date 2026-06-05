package com.contractops.api.common.security

enum class AppRole(val authority: String) {
    ADMIN("ROLE_ADMIN"),
    GESTOR_GRUPO("ROLE_GESTOR_GRUPO"),
    GESTOR_CONTRATO("ROLE_GESTOR_CONTRATO"),
    SUPERVISOR("ROLE_SUPERVISOR"),
    DP("ROLE_DP"),
    FINANCEIRO("ROLE_FINANCEIRO"),
    CONTADOR("ROLE_CONTADOR"),
    FISCAL_INTERNO("ROLE_FISCAL_INTERNO"),
    PORTAL_ORGAO("ROLE_PORTAL_ORGAO");

    companion object {
        fun fromAuthority(value: String): AppRole? =
            entries.find { it.authority == value || it.name == value }
    }
}
