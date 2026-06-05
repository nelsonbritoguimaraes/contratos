package com.contractops.api.ia.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "contractops.kafka")
data class KafkaProperties(
    var enabled: Boolean = false,
    var bootstrapServers: String = "localhost:9092",
    var topic: String = "contractops.domain-events"
)

@Configuration
@EnableConfigurationProperties(AiProperties::class, KafkaProperties::class)
class IaIntegrationConfig
