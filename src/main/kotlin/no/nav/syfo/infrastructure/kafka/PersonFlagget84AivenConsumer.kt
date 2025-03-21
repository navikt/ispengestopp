package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.application.Environment
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun kafkaPersonFlaggetAivenConsumerProperties(
    environment: Environment,
) = Properties().apply {
    putAll(commonKafkaAivenConsumerConfig(environment.kafka))
    this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
}

fun createPersonFlagget84AivenConsumer(
    env: Environment,
) = KafkaConsumer<String, String>(kafkaPersonFlaggetAivenConsumerProperties(env)).also {
    it.subscribe(listOf(env.stoppAutomatikkAivenTopic))
}
