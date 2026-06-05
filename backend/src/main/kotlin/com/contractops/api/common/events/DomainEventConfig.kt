package com.contractops.api.common.events

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class DomainEventConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "contractops.kafka", name = ["enabled"], havingValue = "true")
    fun kafkaDomainEventPublisher(
        kafkaPublisher: KafkaDomainEventPublisher
    ): DomainEventPublisher = kafkaPublisher

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "contractops.kafka", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    fun inMemoryDomainEventPublisher(
        publisher: InMemoryDomainEventPublisher
    ): DomainEventPublisher = publisher
}
