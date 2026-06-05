package com.contractops.api.common.events

interface DomainEventPublisher {
    fun publish(event: DomainEvent)
}
