package com.contractops.api.financeiro.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "cobrancas")
class Cobranca(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) val tenantId: UUID,
    @Column(name = "conta_a_receber_id", nullable = false) val contaAReceberId: UUID,
    @Column(name = "tipo", nullable = false, length = 20) var tipo: String,
    @Column(name = "codigo_pix", columnDefinition = "TEXT") var codigoPix: String? = null,
    @Column(name = "qr_code_payload", columnDefinition = "TEXT") var qrCodePayload: String? = null,
    @Column(name = "linha_digitavel", length = 120) var linhaDigitavel: String? = null,
    @Column(name = "nosso_numero", length = 40) var nossoNumero: String? = null,
    @Column(name = "status", nullable = false, length = 30) var status: String = "EMITIDA",
    @Column(name = "valor", precision = 16, scale = 2, nullable = false) var valor: BigDecimal,
    @Column(name = "vencimento") var vencimento: LocalDate? = null,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime = OffsetDateTime.now()
)
