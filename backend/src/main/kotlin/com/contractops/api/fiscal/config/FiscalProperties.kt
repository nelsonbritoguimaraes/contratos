package com.contractops.api.fiscal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "contractops.fiscal")
data class FiscalProperties(
    /** stub | sandbox | production */
    var mode: String = "sandbox",
    var esocial: EsocialFiscalProps = EsocialFiscalProps(),
    var nfse: NfseFiscalProps = NfseFiscalProps(),
    var sped: SpedFiscalProps = SpedFiscalProps()
) {
    fun resolvedMode(): FiscalMode = FiscalMode.from(mode)

    data class EsocialFiscalProps(
        /** 1=produção, 2=produção restrita */
        var tpAmb: Int = 2,
        var employerCnpj: String = "00000000000000",
        var transmitUrl: String = "https://webservices.producaorestrita.esocial.gov.br/servicos/empregador/enviarloteeventos/WsEnviarLoteEventos.svc",
        var certificatePath: String? = null,
        var certificatePassword: String? = null
    )

    data class NfseFiscalProps(
        var municipioIbge: String = "3550308",
        var emitUrl: String = "https://sefin.nfse.gov.br/SefinNacional/nfse",
        var prestadorCnpj: String = "00000000000000",
        var certificatePath: String? = null,
        var certificatePassword: String? = null
    )

    data class SpedFiscalProps(
        var layoutVersion: String = "9.0.0",
        var companyCnpj: String = "00000000000000"
    )
}

enum class FiscalMode {
    STUB, SANDBOX, PRODUCTION;

    companion object {
        fun from(value: String): FiscalMode = when (value.lowercase()) {
            "production", "prod" -> PRODUCTION
            "stub" -> STUB
            else -> SANDBOX
        }
    }
}
