package com.contractops.api.contabilidade.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "sped_transmissions")
class SpedTransmission(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "tipo", nullable = false, length = 30)
    var tipo: String,

    @Column(name = "competencia_inicio")
    var competenciaInicio: LocalDate? = null,

    @Column(name = "competencia_fim")
    var competenciaFim: LocalDate? = null,

    @Column(name = "ano_calendario")
    var anoCalendario: Int? = null,

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "RASCUNHO",

    @Column(name = "arquivo_hash", length = 64)
    var arquivoHash: String? = null,

    @Column(name = "total_registros")
    var totalRegistros: Int? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "erros_validacao", columnDefinition = "jsonb")
    var errosValidacao: List<String>? = null,

    @Column(name = "protocolo", length = 100)
    var protocolo: String? = null,

    @Column(name = "mensagem", columnDefinition = "TEXT")
    var mensagem: String? = null,

    @Column(name = "aprovado_por", length = 150)
    var aprovadoPor: String? = null,

    @Column(name = "transmitido_em")
    var transmitidoEm: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
