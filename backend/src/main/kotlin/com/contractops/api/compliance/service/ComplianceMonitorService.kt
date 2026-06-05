package com.contractops.api.compliance.service

import com.contractops.api.compliance.repository.CertificateVaultRefRepository
import com.contractops.api.financeiro.repository.NotaFiscalServicoRepository
import com.contractops.api.rh.repository.EsocialEventRepository
import com.contractops.api.time.repository.TimeClockDeviceRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class ComplianceMonitorService(
    private val certificateRepository: CertificateVaultRefRepository,
    private val clockDeviceRepository: TimeClockDeviceRepository,
    private val esocialEventRepository: EsocialEventRepository,
    private val notaFiscalRepository: NotaFiscalServicoRepository
) {
    data class ComplianceMonitor(
        val id: String,
        val category: String,
        val severity: String,
        val title: String,
        val description: String,
        val entityType: String? = null,
        val entityId: UUID? = null,
        val detectedAt: String = OffsetDateTime.now().toString()
    )

    fun listMonitors(tenantId: UUID): List<ComplianceMonitor> {
        val monitors = mutableListOf<ComplianceMonitor>()
        val today = LocalDate.now()
        val offlineThreshold = OffsetDateTime.now().minusHours(24)

        certificateRepository.findByTenantIdAndActiveTrue(tenantId).forEach { cert ->
            cert.expiresAt?.let { exp ->
                val days = ChronoUnit.DAYS.between(today, exp)
                if (days <= 30) {
                    monitors.add(
                        ComplianceMonitor(
                            id = "cert-${cert.id}",
                            category = "CERTIFICADO",
                            severity = if (days <= 7) "CRITICAL" else "WARNING",
                            title = "Certificado ${cert.alias} expira em $days dia(s)",
                            description = "CNPJ ${cert.cnpj} — validade $exp",
                            entityType = "CERTIFICATE",
                            entityId = cert.id
                        )
                    )
                }
            }
        }

        clockDeviceRepository.findByTenantId(tenantId)
            .filter { it.status == "OFFLINE" || it.status == "ERROR" || it.lastSyncAt == null || it.lastSyncAt!!.isBefore(offlineThreshold) }
            .forEach { dev ->
                monitors.add(
                    ComplianceMonitor(
                        id = "clock-${dev.id}",
                        category = "RELOGIO_PONTO",
                        severity = "WARNING",
                        title = "Relógio offline: ${dev.name}",
                        description = "Última sync: ${dev.lastSyncAt ?: "nunca"} — status ${dev.status}",
                        entityType = "CLOCK_DEVICE",
                        entityId = dev.id
                    )
                )
            }

        esocialEventRepository.findByTenantIdAndStatus(tenantId, "REJECTED").take(20).forEach { ev ->
            monitors.add(
                ComplianceMonitor(
                    id = "esocial-${ev.id}",
                    category = "ESOCIAL",
                    severity = "CRITICAL",
                    title = "eSocial rejeitado: ${ev.eventType}",
                    description = "Evento ${ev.id} — competência ${ev.competence}",
                    entityType = "ESOCIAL_EVENT",
                    entityId = ev.id
                )
            )
        }

        notaFiscalRepository.findByTenantId(tenantId)
            .filter { it.status.contains("ERRO", ignoreCase = true) || it.status.contains("FALHA", ignoreCase = true) || it.observacoes?.contains("falha", ignoreCase = true) == true }
            .take(20)
            .forEach { nfs ->
                monitors.add(
                    ComplianceMonitor(
                        id = "nfse-${nfs.id}",
                        category = "NFSE",
                        severity = "WARNING",
                        title = "Falha NFS-e ${nfs.numero}",
                        description = nfs.observacoes ?: "Status ${nfs.status}",
                        entityType = "NFS_E",
                        entityId = nfs.id
                    )
                )
            }

        return monitors.sortedBy { when (it.severity) { "CRITICAL" -> 0; "WARNING" -> 1; else -> 2 } }
    }
}
