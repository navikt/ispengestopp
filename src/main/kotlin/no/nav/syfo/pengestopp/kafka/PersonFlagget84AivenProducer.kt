package no.nav.syfo.pengestopp.kafka

import no.nav.syfo.application.Environment
import no.nav.syfo.pengestopp.StatusEndring
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

fun kafkaPersonFlaggetAivenProducerProperties(
    environment: Environment,
): Properties {
    return Properties().apply {
        putAll(commonKafkaAivenConfig(environment.kafka))
        this[ProducerConfig.ACKS_CONFIG] = "all"
        this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        this[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "1"
        this[ProducerConfig.MAX_BLOCK_MS_CONFIG] = "15000"
        this[ProducerConfig.RETRIES_CONFIG] = "100000"
        this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.canonicalName
        this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java.canonicalName
    }
}

fun createPersonFlagget84AivenProducer(
    env: Environment,
): KafkaProducer<String, StatusEndring> {
    val kafkaProducerPropertes = kafkaPersonFlaggetAivenProducerProperties(env)
    return KafkaProducer(kafkaProducerPropertes)
}
