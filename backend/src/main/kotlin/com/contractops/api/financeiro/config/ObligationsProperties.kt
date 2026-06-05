package com.contractops.api.financeiro.config

import com.contractops.api.fiscal.config.FiscalMode
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "contractops.obligations")
data class ObligationsProperties(
    var mode: String = "sandbox",
    var fgts: FgtsProps = FgtsProps(),
    var dctfweb: DctfWebProps = DctfWebProps()
) {
    fun resolvedMode(): FiscalMode = FiscalMode.from(mode)

    data class FgtsProps(
        var apiUrl: String = "https://api.fgtsdigital.serpro.gov.br/v1",
        var certificatePath: String? = null,
        var certificatePassword: String? = null
    )

    data class DctfWebProps(
        var transmitUrl: String = "https://www.esocial.gov.br/webservices/empregador/dctfweb",
        var certificatePath: String? = null,
        var certificatePassword: String? = null
    )
}

@ConfigurationProperties(prefix = "contractops.openfinance")
data class OpenFinanceProperties(
    var webhookSecret: String? = null,
    var enabled: Boolean = true,
    var authorizationBaseUrl: String = "https://openfinance.sandbox.example",
    var consentPath: String = "/consent/{consentId}",
    var institutionQueryParam: String = "institution",
    var tenantQueryParam: String = "tenant"
) {
    fun buildAuthorizationUrl(consentId: String, institutionId: String, tenantId: java.util.UUID): String {
        val path = consentPath.replace("{consentId}", consentId)
        val base = authorizationBaseUrl.trimEnd('/')
        return "$base$path?${institutionQueryParam}=$institutionId&${tenantQueryParam}=$tenantId"
    }
}

@ConfigurationProperties(prefix = "contractops.nfs-workflow")
data class NfsWorkflowProperties(
    var autoStartOnEmit: Boolean = false,
    var autoEmailOnEmit: Boolean = true,
    var orgaoEmailDefault: String = "faturamento@orgao.gov.br"
)
