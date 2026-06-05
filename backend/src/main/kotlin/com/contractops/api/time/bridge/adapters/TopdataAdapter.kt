package com.contractops.api.time.bridge.adapters

import com.contractops.api.time.bridge.ClockBridgeAdapter
import com.contractops.api.time.bridge.ClockVendor
import com.contractops.api.time.bridge.GenericAfdAdapter
import com.contractops.api.time.domain.RawPunch
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.Socket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class TopdataAdapter(private val genericAfdAdapter: GenericAfdAdapter) : ClockBridgeAdapter {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun getVendor(): ClockVendor = ClockVendor.TOPDATA

    override fun importPunches(content: String, deviceId: UUID?, tenantId: UUID): ClockBridgeAdapter.ImportResult {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseStructuredPayload(trimmed, deviceId, tenantId)
        }
        return genericAfdAdapter.importPunches(content, deviceId, tenantId).copy(vendor = getVendor())
    }

    override fun fetchFromDevice(apiUrl: String, deviceId: UUID?, tenantId: UUID): ClockBridgeAdapter.ImportResult {
        return try {
            val uri = java.net.URI(apiUrl)
            val host = uri.host ?: apiUrl.substringBefore(":")
            val port = if (uri.port > 0) uri.port else 3000

            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), 5000)
                socket.soTimeout = 8000
                val out = socket.getOutputStream()
                out.write("GET_PUNCHES\n".toByteArray())
                out.flush()
                val response = socket.getInputStream().bufferedReader().readText()
                if (response.isBlank()) {
                    return stubTcpPunches(deviceId, tenantId, listOf("TCP vazio — stub Topdata"))
                }
                if (response.trim().startsWith("{") || response.trim().startsWith("[")) {
                    parseStructuredPayload(response, deviceId, tenantId)
                } else {
                    parseTopdataCsv(response, deviceId, tenantId)
                }
            }
        } catch (ex: Exception) {
            stubTcpPunches(deviceId, tenantId, listOf("Topdata TCP stub: ${ex.message}"))
        }
    }

    private fun parseStructuredPayload(content: String, deviceId: UUID?, tenantId: UUID): ClockBridgeAdapter.ImportResult {
        val errors = mutableListOf<String>()
        val punches = mutableListOf<RawPunch>()
        val root = mapper.readTree(content)
        val nodes = when {
            root.isArray -> root
            root.has("records") -> root["records"]
            root.has("punches") -> root["punches"]
            else -> mapper.createArrayNode().add(root)
        }

        nodes.forEach { node ->
            try {
                val ts = LocalDateTime.parse(
                    node.path("datetime").asText(node.path("timestamp").asText()),
                    formatter
                )
                punches.add(
                    RawPunch(
                        tenantId = tenantId,
                        deviceId = deviceId,
                        matricula = node.path("badge").asText(null),
                        cpf = node.path("cpf").asText(null),
                        punchTimestamp = ts,
                        punchType = node.path("direction").asText("ENTRADA"),
                        nsr = node.path("nsr").asText(null),
                        rawData = node.toString(),
                        sourceChannel = "TOPDATA_API"
                    )
                )
            } catch (ex: Exception) {
                errors.add("Topdata JSON inválido: ${ex.message}")
            }
        }

        return ClockBridgeAdapter.ImportResult(punches, errors, nodes.size(), getVendor())
    }

    private fun parseTopdataCsv(content: String, deviceId: UUID?, tenantId: UUID): ClockBridgeAdapter.ImportResult {
        val errors = mutableListOf<String>()
        val punches = content.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split(";")
                if (parts.size < 3) {
                    errors.add("Linha CSV inválida: $line")
                    null
                } else {
                    RawPunch(
                        tenantId = tenantId,
                        deviceId = deviceId,
                        matricula = parts[0].trim(),
                        cpf = parts.getOrNull(1)?.trim(),
                        punchTimestamp = LocalDateTime.parse(parts[2].trim(), formatter),
                        punchType = parts.getOrNull(3)?.trim() ?: "ENTRADA",
                        rawData = line,
                        sourceChannel = "TOPDATA_TCP"
                    )
                }
            }.toList()

        return ClockBridgeAdapter.ImportResult(punches, errors, punches.size, getVendor())
    }

    private fun stubTcpPunches(deviceId: UUID?, tenantId: UUID, errors: List<String>): ClockBridgeAdapter.ImportResult {
        log.warn("Using stub TCP punches — real Topdata clock not available")
        val now = LocalDateTime.now()
        val punches = listOf(
            RawPunch(
                tenantId = tenantId, deviceId = deviceId, matricula = "TD-100",
                punchTimestamp = now.withHour(6).withMinute(55), punchType = "ENTRADA",
                nsr = "TD-${System.currentTimeMillis()}",
                rawData = """{"source":"topdata_tcp_stub"}""", sourceChannel = "TOPDATA_STUB"
            ),
            RawPunch(
                tenantId = tenantId, deviceId = deviceId, matricula = "TD-100",
                punchTimestamp = now.withHour(19).withMinute(5), punchType = "SAIDA",
                nsr = "TD-${System.currentTimeMillis() + 1}",
                rawData = """{"source":"topdata_tcp_stub"}""", sourceChannel = "TOPDATA_STUB"
            )
        )
        return ClockBridgeAdapter.ImportResult(punches, errors, punches.size, getVendor())
    }
}
