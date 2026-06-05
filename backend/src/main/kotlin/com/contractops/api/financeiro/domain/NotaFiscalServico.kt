package com.contractops.api.financeiro.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Nota Fiscal de Serviços Eletrônica (NFS-e).
 * Emissão, controle de retenções e integração com AR.
 * SPEC §16.2 + §22
 */
@Entity
@Table(name = "notas_fiscais_servico")
class NotaFiscalServico(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "numero", length = 20, nullable = false)
    var numero: String,

    @Column(name = "serie", length = 10, nullable = false)
    var serie: String = "1",

    @Column(name = "codigo_verificacao", length = 100)
    var codigoVerificacao: String? = null,

    @Column(name = "data_emissao", nullable = false)
    var dataEmissao: LocalDate,

    @Column(name = "tomador_cnpj", length = 20, nullable = false)
    var tomadorCnpj: String,

    @Column(name = "tomador_razao_social", length = 200)
    var tomadorRazaoSocial: String? = null,

    @Column(name = "contrato_id")
    val contratoId: UUID? = null,

    @Column(name = "measurement_id")
    val measurementId: UUID? = null,

    @Column(name = "valor_servicos", precision = 16, scale = 2, nullable = false)
    var valorServicos: BigDecimal,

    @Column(name = "valor_liquido", precision = 16, scale = 2, nullable = false)
    var valorLiquido: BigDecimal,

    @Column(name = "iss_retido", precision = 16, scale = 2, nullable = false)
    var issRetido: BigDecimal = BigDecimal.ZERO,

    @Column(name = "outras_retencoes", precision = 16, scale = 2, nullable = false)
    var outrasRetencoes: BigDecimal = BigDecimal.ZERO,

    @Column(name = "xml", columnDefinition = "TEXT")
    var xml: String? = null,                  // XML completo da NFS-e

    @Column(name = "pdf_url", length = 500)
    var pdfUrl: String? = null,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "EMITIDA",           // EMITIDA, CANCELADA, SUBSTITUIDA

    @Column(name = "protocolo", length = 100)
    var protocolo: String? = null,

    @Column(name = "observacoes", columnDefinition = "TEXT")
    var observacoes: String? = null,

    @Column(name = "tomador_email", length = 200)
    var tomadorEmail: String? = null,

    @Column(name = "danfse_html", columnDefinition = "TEXT")
    var danfseHtml: String? = null,

    @Column(name = "email_orgao_enviado_em")
    var emailOrgaoEnviadoEm: java.time.OffsetDateTime? = null,

    @Column(name = "reinf_protocolo", length = 100)
    var reinfProtocolo: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "NotaFiscalServico(id=$id, numero='$numero', valorLiquido=$valorLiquido, status='$status')"
}