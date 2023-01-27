package no.nav.syfo.pengestopp.kafka

import no.nav.syfo.application.Environment
import no.nav.syfo.application.kafka.commonKafkaAivenConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun kafkaPersonFlaggetAivenConsumerProperties(
    environment: Environment,
) = Properties().apply {
    putAll(commonKafkaAivenConsumerConfig(environment.kafka))
    this[ConsumerConfig.GROUP_ID_CONFIG] = "ispengestopp-v1"
    this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
}

fun createPersonFlagget84AivenConsumer(
    env: Environment,
) = KafkaConsumer<String, String>(kafkaPersonFlaggetAivenConsumerProperties(env)).also {
    it.subscribe(listOf(env.stoppAutomatikkAivenTopic))
}
