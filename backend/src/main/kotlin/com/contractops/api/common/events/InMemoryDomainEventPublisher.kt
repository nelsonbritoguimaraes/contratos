package com.contractops.api.common.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Fallback in-memory event bus for dev/test and when Kafka is disabled.
 */
@Component
class InMemoryDomainEventPublisher : DomainEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)
    val events = CopyOnWriteArrayList<DomainEvent>()

    override fun publish(event: DomainEvent) {
        events.add(event)
        log.info("[DomainEvent/InMemory] {} tenant={} entity={}/{}", event.eventType, event.tenantId, event.entityType, event.entityId)
    }

    fun recent(limit: Int = 50): List<DomainEvent> = events.takeLast(limit)
}
