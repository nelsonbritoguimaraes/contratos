package com.contractops.api.time.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.time.domain.AttendanceDay
import com.contractops.api.time.domain.PunchAdjustment
import com.contractops.api.time.domain.RawPunch
import com.contractops.api.time.service.*
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/time-punches")
class TimePunchController(
    private val timePunchService: TimePunchService,
    private val attendanceProcessingService: AttendanceProcessingService,
    private val afdImportService: AfdImportService,
    private val clockBridgeService: com.contractops.api.time.bridge.ClockBridgeService,
    private val aejExportService: AejExportService,
    private val pipelineService: TimePunchPipelineService,
    private val pontoPayrollIntegration: PontoPayrollIntegrationService,
    private val punchAdjustmentService: PunchAdjustmentService,
    private val punchComprovanteService: PunchComprovanteService,
    private val pontoOcrService: PontoOcrService,
    private val employeePunchResolver: EmployeePunchResolver
) {

    private fun tenant(tenantId: UUID?) = tenantId ?: TenantContext.getCurrentTenantId()
        ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

    @GetMapping
    fun listByPeriod(
        @RequestParam start: String,
        @RequestParam end: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<RawPunch>> {
        val punches = timePunchService.findPunchesByPeriod(
            tenant(tenantId), LocalDateTime.parse(start), LocalDateTime.parse(end)
        )
        return ResponseEntity.ok(punches)
    }

    @PostMapping("/import")
    fun importPunches(
        @RequestBody punches: List<RawPunch>,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val tid = tenant(tenantId)
        val normalized = punches.map { p ->
            RawPunch(
                tenantId = tid, deviceId = p.deviceId, employeeId = p.employeeId,
                matricula = p.matricula, cpf = p.cpf, punchTimestamp = p.punchTimestamp,
                punchType = p.punchType, nsr = p.nsr, rawData = p.rawData,
                latitude = p.latitude, longitude = p.longitude, sourceChannel = p.sourceChannel
            )
        }
        val imported = timePunchService.importRawPunches(normalized, tid)
        return ResponseEntity.ok(mapOf("imported" to imported, "total_received" to punches.size))
    }

    @PostMapping("/import-afd", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importAfd(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) deviceId: UUID?,
        @RequestParam(required = false) contractId: UUID?,
        @RequestParam(defaultValue = "true") autoProcess: Boolean,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val tid = tenant(tenantId)
        val result = pipelineService.importAfdAndProcess(file, tid, deviceId, contractId, autoProcess)
        return ResponseEntity.ok(
            mapOf(
                "imported" to result.imported,
                "total_parsed" to result.totalParsed,
                "formato" to result.formato,
                "errors" to result.errors,
                "processed_days" to result.processedDays,
                "employees_processed" to result.employeesProcessed,
                "file_name" to (file.originalFilename ?: file.name)
            )
        )
    }

    @PostMapping("/process-day")
    fun processDay(
        @RequestParam employeeId: UUID,
        @RequestParam date: String,
        @RequestParam(required = false) postId: UUID?,
        @RequestParam(required = false) contractId: UUID?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<AttendanceDay> {
        val attendance = attendanceProcessingService.processEmployeeDay(
            employeeId, LocalDate.parse(date), tenant(tenantId), postId, contractId
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(attendance)
    }

    @PostMapping("/process-month")
    fun processMonth(
        @RequestParam contractId: UUID,
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val result = pipelineService.processContractMonth(tenant(tenantId), contractId, LocalDate.parse(competencia))
        return ResponseEntity.ok(
            mapOf("processed_days" to result.processedDays, "employees_processed" to result.employeesProcessed)
        )
    }

    @GetMapping("/daily-summary")
    fun getDailySummary(
        @RequestParam contractId: UUID,
        @RequestParam date: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(
            attendanceProcessingService.getDailyCoverageSummary(contractId, LocalDate.parse(date), tenant(tenantId))
        )

    @GetMapping("/monthly-summary")
    fun monthlySummary(
        @RequestParam employeeId: UUID,
        @RequestParam contractId: UUID,
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(
            attendanceProcessingService.getMonthlyEmployeeSummary(
                employeeId, contractId, LocalDate.parse(competencia), tenant(tenantId)
            )
        )

    @PostMapping("/gerar-eventos-folha")
    fun gerarEventosFolha(
        @RequestParam employeeId: UUID,
        @RequestParam contractId: UUID,
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val events = pontoPayrollIntegration.gerarEventosMensais(
            employeeId, contractId, LocalDate.parse(competencia), tenant(tenantId)
        )
        return ResponseEntity.ok(events)
    }

    @GetMapping("/espelho")
    fun espelhoPonto(
        @RequestParam employeeId: UUID,
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(
            attendanceProcessingService.gerarEspelhoPonto(
                employeeId, LocalDate.parse(competencia).withDayOfMonth(1), tenant(tenantId)
            )
        )

    @GetMapping("/espelho/text")
    fun espelhoTexto(
        @RequestParam employeeId: UUID,
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val esp = attendanceProcessingService.gerarEspelhoPonto(
            employeeId, LocalDate.parse(competencia).withDayOfMonth(1), tenant(tenantId)
        )
        val sb = StringBuilder()
        sb.appendLine(esp["titulo"])
        sb.appendLine("Colaborador: ${esp["nome"]} CPF: ${esp["cpf"]}")
        sb.appendLine("Competência: ${esp["competencia"]}")
        @Suppress("UNCHECKED_CAST")
        (esp["linhas"] as List<Map<String, Any?>>).forEach { l ->
            sb.appendLine("${l["data"]} | ${l["entrada"]} - ${l["saida"]} | ${l["minutosTrabalhados"]} min")
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"espelho.txt\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(sb.toString())
    }

    @PostMapping("/mobile-punch")
    fun mobilePunch(
        @RequestBody req: MobilePunchRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any?>> {
        val tid = tenant(tenantId)
        val emp = employeePunchResolver.resolve(tid, req.matricula, req.cpf)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Colaborador não encontrado"))
        val assignment = employeePunchResolver.activeAssignment(tid, emp.id!!)
        val punch = RawPunch(
            tenantId = tid,
            employeeId = emp.id,
            matricula = emp.matricula,
            cpf = employeePunchResolver.normalizeCpf(emp.cpf),
            punchTimestamp = req.timestamp ?: LocalDateTime.now(),
            punchType = req.punchType ?: "ENTRADA",
            nsr = "MOB-${System.currentTimeMillis()}",
            rawData = req.rawData,
            latitude = req.latitude,
            longitude = req.longitude,
            sourceChannel = "MOBILE_REP_P"
        )
        val importResult = timePunchService.importRawPunchesDetailed(listOf(punch), tid)
        val savedPunch = importResult.saved.firstOrNull() ?: punch
        val comprovante = punchComprovanteService.emitir(savedPunch, emp.id!!)
        attendanceProcessingService.processEmployeeDay(
            emp.id!!, savedPunch.punchTimestamp.toLocalDate(), tid,
            assignment?.postId, assignment?.contractId
        )
        return ResponseEntity.ok(
            mapOf<String, Any?>(
                "punchId" to savedPunch.id,
                "comprovanteId" to comprovante.id,
                "hash" to comprovante.hashComprovante,
                "timestamp" to savedPunch.punchTimestamp.toString()
            )
        )
    }

    @GetMapping("/comprovantes")
    fun comprovantes48h(
        @RequestParam employeeId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(punchComprovanteService.listUltimas48h(tenant(tenantId), employeeId))

    @PostMapping("/ocr-import")
    fun ocrImport(
        @RequestBody req: Map<String, String>,
        @RequestParam(required = false) deviceId: UUID?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val tid = tenant(tenantId)
        val text = req["text"] ?: return ResponseEntity.badRequest().build()
        val parsed = pontoOcrService.extrairMarcacoesDeTextoOcr(text, deviceId, tid)
        val imported = timePunchService.importRawPunches(parsed.punches, tid)
        return ResponseEntity.ok(mapOf("imported" to imported, "formato" to parsed.formato, "errors" to parsed.errors))
    }

    @GetMapping("/volantes")
    fun volantes(
        @RequestParam contractId: UUID,
        @RequestParam date: String,
        @RequestParam(required = false) tenantId: UUID?
    ) = ResponseEntity.ok(
        attendanceProcessingService.detectVolantes(tenant(tenantId), contractId, LocalDate.parse(date))
    )

    @GetMapping("/adjustments/pending")
    fun adjustmentsPending(@RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(punchAdjustmentService.listPending(tenant(tenantId)))

    @PostMapping("/adjustments")
    fun createAdjustment(@RequestBody req: PunchAdjustmentRequest, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(
            punchAdjustmentService.solicitar(
                PunchAdjustment(
                    tenantId = tenant(req.tenantId),
                    employeeId = req.employeeId,
                    contractId = req.contractId,
                    postId = req.postId,
                    date = LocalDate.parse(req.date),
                    tipo = req.tipo,
                    motivo = req.motivo,
                    evidenciaUrl = req.evidenciaUrl,
                    solicitadoPor = req.solicitadoPor
                )
            )
        )

    @PostMapping("/adjustments/{id}/aprovar-supervisor")
    fun aprovarSupervisor(@PathVariable id: UUID, @RequestParam aprovador: String) =
        ResponseEntity.ok(punchAdjustmentService.aprovarSupervisor(id, aprovador))

    @PostMapping("/adjustments/{id}/aprovar-dp")
    fun aprovarDp(@PathVariable id: UUID, @RequestParam aprovador: String) =
        ResponseEntity.ok(punchAdjustmentService.aprovarDp(id, aprovador))

    @PostMapping("/adjustments/{id}/rejeitar")
    fun rejeitar(@PathVariable id: UUID, @RequestParam motivo: String) =
        ResponseEntity.ok(punchAdjustmentService.rejeitar(id, motivo))

    @GetMapping("/export-aej")
    fun exportAej(
        @RequestParam contractId: UUID,
        @RequestParam period: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val localPeriod = LocalDate.parse(period)
        val content = aejExportService.generateAej(contractId, localPeriod, tenant(tenantId))
        val p7s = aejExportService.generateAejP7s(contractId, localPeriod, tenant(tenantId))
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aej_${contractId}.txt\"")
            .header("X-P7S-Filename", p7s.filename)
            .header("X-P7S-Mode", p7s.mode)
            .header("X-P7S-Signature", p7s.signatureBase64)
            .contentType(MediaType.TEXT_PLAIN)
            .body(content)
    }

    @GetMapping("/export-aej-p7s")
    fun exportAejP7s(
        @RequestParam contractId: UUID,
        @RequestParam period: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ByteArray> {
        val localPeriod = LocalDate.parse(period)
        val p7s = aejExportService.generateAejP7s(contractId, localPeriod, tenant(tenantId))
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${p7s.filename}\"")
            .header("X-P7S-Mode", p7s.mode)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(Base64.getDecoder().decode(p7s.signatureBase64))
    }

    @PostMapping("/bridge/import")
    fun importViaClockBridge(
        @RequestParam deviceId: UUID,
        @RequestParam(required = false) vendor: String?,
        @RequestBody content: String,
        @RequestParam(required = false) contractId: UUID?,
        @RequestParam(defaultValue = "true") autoProcess: Boolean,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val tid = tenant(tenantId)
        val result = clockBridgeService.importFromDevice(deviceId, content, tid)
        val imported = timePunchService.importRawPunches(result.punches, tid)
        val batch = if (autoProcess) pipelineService.processImportedPunches(tid, contractId, result.punches)
        else TimePunchPipelineService.BatchProcessResult(0, 0)
        return ResponseEntity.ok(
            mapOf(
                "vendor" to result.vendor.name,
                "imported" to imported,
                "errors" to result.errors,
                "total_parsed" to result.totalLines,
                "processed_days" to batch.processedDays
            )
        )
    }

    @GetMapping("/bridge/vendors")
    fun listSupportedVendors() = ResponseEntity.ok(
        clockBridgeService.getSupportedVendors().map { mapOf("name" to it.name, "displayName" to it.displayName) }
    )
}

data class MobilePunchRequest(
    val matricula: String? = null,
    val cpf: String? = null,
    val punchType: String? = null,
    val timestamp: LocalDateTime? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val rawData: String? = null
)

data class PunchAdjustmentRequest(
    val tenantId: UUID? = null,
    val employeeId: UUID,
    val contractId: UUID? = null,
    val postId: UUID? = null,
    val date: String,
    val tipo: String,
    val motivo: String,
    val evidenciaUrl: String? = null,
    val solicitadoPor: String? = null
)
