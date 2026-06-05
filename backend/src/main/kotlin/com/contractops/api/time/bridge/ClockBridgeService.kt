package com.contractops.api.time.bridge

import com.contractops.api.time.domain.RawPunch
import com.contractops.api.time.domain.TimeClockDevice
import com.contractops.api.time.repository.TimeClockDeviceRepository
import org.springframework.stereotype.Service
import java.util.*

/**
 * Clock Bridge — camada de abstração para múltiplos fabricantes de relógios de ponto.
 * 
 * Responsável por:
 * - Roteamento automático baseado no vendor do dispositivo
 * - Suporte a importação unificada (AFD, CSV, API futura)
 * - Extensibilidade para novos fabricantes sem alterar o core
 *
 * Esta é a fundação da Fase 2 do Roadmap (SPEC §10 + §30).
 */
@Service
class ClockBridgeService(
    private val adapters: List<ClockBridgeAdapter>,
    private val timeClockDeviceRepository: TimeClockDeviceRepository
) {

    /**
     * Importa marcações usando o adaptador mais adequado para o dispositivo.
     */
    fun importFromDevice(
        deviceId: UUID,
        content: String,
        tenantId: UUID
    ): ClockBridgeAdapter.ImportResult {
        val device = timeClockDeviceRepository.findById(deviceId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Dispositivo não encontrado") }

        val vendor = ClockVendor.fromName(device.model) // model pode armazenar o vendor
        val adapter = findAdapterForVendor(vendor)

        return adapter.importPunches(content, deviceId, tenantId)
    }

    /**
     * Importação genérica (quando não se sabe o vendor de antemão).
     */
    fun importGeneric(
        content: String,
        deviceId: UUID?,
        tenantId: UUID,
        preferredVendor: ClockVendor? = null
    ): ClockBridgeAdapter.ImportResult {
        val vendor = preferredVendor ?: ClockVendor.GENERIC_AFD
        val adapter = findAdapterForVendor(vendor)
        return adapter.importPunches(content, deviceId, tenantId)
    }

    private fun findAdapterForVendor(vendor: ClockVendor): ClockBridgeAdapter {
        return adapters.firstOrNull { it.getVendor() == vendor }
            ?: adapters.first { it.getVendor() == ClockVendor.GENERIC_AFD }
    }

    fun getSupportedVendors(): List<ClockVendor> {
        return adapters.map { it.getVendor() }
    }
}