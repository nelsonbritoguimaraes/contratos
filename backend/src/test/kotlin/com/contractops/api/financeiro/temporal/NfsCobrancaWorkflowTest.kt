package com.contractops.api.financeiro.temporal

import com.contractops.api.financeiro.service.FinanceWorkflowOrchestratorService
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.client.WorkflowStub
import io.temporal.testing.TestWorkflowEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class NfsCobrancaWorkflowTest {

    private lateinit var testEnv: TestWorkflowEnvironment
    private val activityLog = CopyOnWriteArrayList<String>()

  @BeforeEach
    fun setUp() {
        testEnv = TestWorkflowEnvironment.newInstance()
        val worker = testEnv.newWorker(TASK_QUEUE)
        worker.registerWorkflowImplementationTypes(NfsCobrancaWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(recordingActivities())
        testEnv.start()
    }

    @AfterEach
    fun tearDown() {
        testEnv.close()
    }

    @Test
    fun `workflow NFS cobranca executa atividades ate concluir`() {
        val wfId = UUID.randomUUID()
        val input = NfsCobrancaWorkflowInput(
            financeWorkflowId = wfId.toString(),
            notaFiscalId = UUID.randomUUID().toString(),
            contaAReceberId = UUID.randomUUID().toString(),
            tenantId = UUID.randomUUID().toString(),
            aguardarRecebimentoDias = 0
        )

        val client = testEnv.workflowClient
        val stub = client.newWorkflowStub(
            NfsCobrancaWorkflow::class.java,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId("test-nfs-${wfId}")
                .build()
        )

        val result = stub.run(input)

        assertEquals(FinanceWorkflowOrchestratorService.Estado.CONCLUIDO.name, result.estadoFinal)
        assertEquals(wfId.toString(), result.financeWorkflowId)
        assertTrue(activityLog.contains("enviarDanfse"))
        assertTrue(activityLog.contains("marcarConcluido"))
    }

    @Test
    fun `workflow pode ser iniciado de forma assincrona via WorkflowClient start`() {
        val input = NfsCobrancaWorkflowInput(
            financeWorkflowId = UUID.randomUUID().toString(),
            notaFiscalId = UUID.randomUUID().toString(),
            contaAReceberId = null,
            tenantId = UUID.randomUUID().toString(),
            aguardarRecebimentoDias = 1
        )
        val client = testEnv.workflowClient
        val stub = client.newWorkflowStub(
            NfsCobrancaWorkflow::class.java,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId("test-async-${UUID.randomUUID()}")
                .build()
        )
        WorkflowClient.start(stub::run, input)
        testEnv.sleep(java.time.Duration.ofDays(2))
        val untyped = WorkflowStub.fromTyped(stub)
        val result = untyped.getResult(NfsCobrancaWorkflowResult::class.java)
        assertEquals(FinanceWorkflowOrchestratorService.Estado.CONCLUIDO.name, result.estadoFinal)
    }

    private fun recordingActivities(): NfsCobrancaActivities = object : NfsCobrancaActivities {
        override fun enviarDanfseAoOrgao(notaFiscalId: String, tenantId: String, orgaoEmail: String?) {
            activityLog += "enviarDanfse"
        }

        override fun atualizarEstadoWorkflow(
            financeWorkflowId: String,
            tenantId: String,
            estado: String,
            mensagem: String
        ) {
            activityLog += "estado:$estado"
        }

        override fun vincularContaReceber(financeWorkflowId: String, tenantId: String, contaAReceberId: String?) {
            activityLog += "vincularAr"
        }

        override fun marcarProntoConciliacao(financeWorkflowId: String, tenantId: String) {
            activityLog += "conciliacao"
        }

        override fun marcarWorkflowConcluido(financeWorkflowId: String, tenantId: String) {
            activityLog += "marcarConcluido"
        }
    }

    companion object {
        private const val TASK_QUEUE = "contractops-finance-nfs"
    }
}
