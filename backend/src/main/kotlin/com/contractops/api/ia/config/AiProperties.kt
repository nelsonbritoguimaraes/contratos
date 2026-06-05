package com.contractops.api.ia.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "contractops.ai")
data class AiProperties(
    /** stub | sandbox | production */
    var mode: String = "sandbox",
    var opensearch: OpenSearchProps = OpenSearchProps()
) {
    fun isProduction(): Boolean = mode.equals("production", ignoreCase = true)

    data class OpenSearchProps(
        var enabled: Boolean = false,
        var host: String = "http://localhost:9200",
        var index: String = "contractops-rag"
    )
}
