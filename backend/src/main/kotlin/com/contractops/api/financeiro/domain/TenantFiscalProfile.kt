package com.contractops.api.financeiro.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "tenant_fiscal_profile")
class TenantFiscalProfile(
    @Id
    @Column(name = "tenant_id")
    val tenantId: UUID,

    @Column(name = "desoneracao_folha", nullable = false)
    var desoneracaoFolha: Boolean = false,

    @Column(name = "aliquota_inss_retencao", precision = 5, scale = 4, nullable = false)
    var aliquotaInssRetencao: BigDecimal = BigDecimal("0.1100"),

    @Column(name = "simples_nacional", nullable = false)
    var simplesNacional: Boolean = false,

    @Column(name = "municipio_ibge_padrao", length = 10)
    var municipioIbgePadrao: String? = "3550308",

    @Column(name = "cnpj_prestador", length = 18)
    var cnpjPrestador: String? = null,

    @Column(name = "razao_social", length = 255)
    var razaoSocial: String? = null,

    @Column(name = "contador_nome", length = 150)
    var contadorNome: String? = null,

    @Column(name = "contador_cpf", length = 14)
    var contadorCpf: String? = null,

    @Column(name = "contador_crc", length = 20)
    var contadorCrc: String? = null,

    @Column(name = "representante_nome", length = 150)
    var representanteNome: String? = null,

    @Column(name = "representante_cpf", length = 14)
    var representanteCpf: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
