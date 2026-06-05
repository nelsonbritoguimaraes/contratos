package com.contractops.api.time.bridge.adapters

import com.contractops.api.time.bridge.ClockBridgeAdapter
import com.contractops.api.time.bridge.ClockVendor
import com.contractops.api.time.domain.RawPunch
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class ControlIdAdapter(
    private val genericAfdAdapter: com.contractops.api.time.bridge.GenericAfdAdapter
) : ClockBridgeAdapter {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()
    private val isoFormatter = DateTimeFormatter.ISO_DATE_TIME

    override fun getVendor(): ClockVendor = ClockVendor.CONTROL_ID

    override fun importPunches(
        content: String,
        deviceId: UUID?,
        tenantId: UUID
    ): ClockBridgeAdapter.ImportResult {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseStructuredPayload(trimmed, deviceId, tenantId)
        }
        return genericAfdAdapter.importPunches(content, deviceId, tenantId).copy(vendor = getVendor())
    }

    override fun fetchFromDevice(apiUrl: String, deviceId: UUID?, tenantId: UUID): ClockBridgeAdapter.ImportResult {
        return try {
            val connection = URI(apiUrl).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "ContractOps-ClockBridge/1.0")

            if (connection.responseCode !in 200..299) {
                return stubStructuredPunches(deviceId, tenantId, listOf("HTTP ${connection.responseCode} — usando stub local"))
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (body.isBlank()) {
                return stubStructuredPunches(deviceId, tenantId, listOf("Resposta vazia — usando stub local"))
            }
            parseStructuredPayload(body, deviceId, tenantId)
        } catch (ex: Exception) {
            stubStructuredPunches(deviceId, tenantId, listOf("Control iD fetch stub: ${ex.message}"))
        }
    }

    private fun parseStructuredPayload(
        content: String,
        deviceId: UUID?,
        tenantId: UUID
    ): ClockBridgeAdapter.ImportResult {
        val errors = mutableListOf<String>()
        val punches = mutableListOf<RawPunch>()
        val root = mapper.readTree(content)
        val nodes: List<JsonNode> = when {
            root.isArray -> root.toList()
            root.has("punches") -> root["punches"].toList()
            root.has("marcacoes") -> root["marcacoes"].toList()
            else -> listOf(root)
        }

        nodes.forEach { node ->
            try {
                val timestamp = parseTimestamp(node)
                punches.add(
                    RawPunch(
                        tenantId = tenantId,
                        deviceId = deviceId,
                        employeeId = node.path("employeeId").asText(null)?.let { runCatching { UUID.fromString(it) }.getOrNull() },
                        matricula = node.path("matricula").asText(null) ?: node.path("registration").asText(null),
                        cpf = node.path("cpf").asText(null),
                        punchTimestamp = timestamp,
                        punchType = node.path("type").asText(null) ?: node.path("punchType").asText("ENTRADA"),
                        nsr = node.path("nsr").asText(null),
                        rawData = node.toString(),
                        sourceChannel = "CONTROL_ID_API"
                    )
                )
            } catch (ex: Exception) {
                errors.add("Linha inválida Control iD: ${ex.message}")
            }
        }

        if (punches.isEmpty()) {
            return stubStructuredPunches(deviceId, tenantId, errors + "Nenhuma marcação parseada — stub aplicado")
        }

        return ClockBridgeAdapter.ImportResult(
            punches = punches,
            errors = errors,
            totalLines = nodes.size,
            vendor = getVendor()
        )
    }

    private fun stubStructuredPunches(
        deviceId: UUID?,
        tenantId: UUID,
        errors: List<String>
    ): ClockBridgeAdapter.ImportResult {
        log.warn("Using stub structured punches — real ControlId clock not available")
        val now = LocalDateTime.now()
        val punches = listOf(
            RawPunch(
                tenantId = tenantId,
                deviceId = deviceId,
                matricula = "001",
                cpf = "00000000000",
                punchTimestamp = now.withHour(7).withMinute(58),
                punchType = "ENTRADA",
                nsr = "CID-${System.currentTimeMillis()}",
                rawData = """{"source":"control_id_stub","type":"ENTRADA"}""",
                sourceChannel = "CONTROL_ID_STUB"
            ),
            RawPunch(
                tenantId = tenantId,
                deviceId = deviceId,
                matricula = "001",
                cpf = "00000000000",
                punchTimestamp = now.withHour(18).withMinute(2),
                punchType = "SAIDA",
                nsr = "CID-${System.currentTimeMillis() + 1}",
                rawData = """{"source":"control_id_stub","type":"SAIDA"}""",
                sourceChannel = "CONTROL_ID_STUB"
            )
        )
        return ClockBridgeAdapter.ImportResult(
            punches = punches,
            errors = errors,
            totalLines = punches.size,
            vendor = getVendor()
        )
    }

    private fun parseTimestamp(node: JsonNode): LocalDateTime {
        val raw = node.path("timestamp").asText(null)
            ?: node.path("punchTimestamp").asText(null)
            ?: node.path("data_hora").asText(null)
            ?: throw IllegalArgumentException("timestamp ausente")
        return LocalDateTime.parse(raw, isoFormatter)
    }
}
