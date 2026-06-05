package com.contractops.api.financeiro.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "contractops.temporal")
data class TemporalProperties(
    /** Habilita worker Temporal + disparo de workflows reais */
    var enabled: Boolean = false,
    /** gRPC do Temporal Service (ex.: localhost:7233) */
    var target: String = "localhost:7233",
    var namespace: String = "default",
    var taskQueue: String = "contractops-finance-nfs"
)
