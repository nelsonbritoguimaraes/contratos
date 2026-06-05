package com.contractops.api.financeiro.temporal

import com.contractops.api.financeiro.config.TemporalProperties
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "contractops.temporal", name = ["enabled"], havingValue = "true")
class TemporalFinanceConfiguration(
    private val temporalProperties: TemporalProperties,
    private val nfsCobrancaActivities: NfsCobrancaActivitiesImpl
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var workerFactory: WorkerFactory? = null

    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalProperties.target)
                .build()
        )

    @Bean
    fun workflowClient(stubs: WorkflowServiceStubs): WorkflowClient =
        WorkflowClient.newInstance(
            stubs,
            WorkflowClientOptions.newBuilder()
                .setNamespace(temporalProperties.namespace)
                .build()
        )

    @Bean
    fun nfsCobrancaWorkerFactory(client: WorkflowClient): WorkerFactory {
        val factory = WorkerFactory.newInstance(client)
        val worker: Worker = factory.newWorker(temporalProperties.taskQueue)
        worker.registerWorkflowImplementationTypes(NfsCobrancaWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(nfsCobrancaActivities)
        factory.start()
        workerFactory = factory
        log.info(
            "Temporal worker iniciado — fila={}, target={}, namespace={}",
            temporalProperties.taskQueue,
            temporalProperties.target,
            temporalProperties.namespace
        )
        return factory
    }

    @PreDestroy
    fun shutdownWorker() {
        workerFactory?.shutdown()
    }
}
