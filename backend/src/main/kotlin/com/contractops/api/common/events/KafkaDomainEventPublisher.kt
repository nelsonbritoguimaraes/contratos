package com.contractops.api.common.events

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Kafka publisher stub — logs events when Kafka is enabled but client not wired yet.
 * Falls back to in-memory store for observability in dev.
 */
@Component
@ConditionalOnProperty(prefix = "contractops.kafka", name = ["enabled"], havingValue = "true")
class KafkaDomainEventPublisher(
    private val inMemoryFallback: InMemoryDomainEventPublisher
) : DomainEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: DomainEvent) {
        // Stub: real Kafka producer would serialize to topic contractops.domain-events
        log.info(
            "[DomainEvent/KafkaStub] topic=contractops.domain-events type={} tenant={}",
            event.eventType,
            event.tenantId
        )
        inMemoryFallback.publish(event)
    }
}
