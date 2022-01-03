package no.nav.syfo.pengestopp.kafka

import no.nav.syfo.application.Environment
import no.nav.syfo.pengestopp.StatusEndring
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

fun kafkaPersonFlaggetProducerProperties(
    environment: Environment,
): Properties {
    return Properties().apply {
        this["security.protocol"] = "SASL_SSL"
        this["sasl.mechanism"] = "PLAIN"
        this["schema.registry.url"] = "http://kafka-schema-registry.tpa.svc.nais.local:8081"
        this["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${environment.serviceuserUsername}\" password=\"${environment.serviceuserPassword}\";"
        this[ProducerConfig.ACKS_CONFIG] = "all"
        this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true
        this[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 1500
        this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.canonicalName
        this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java.canonicalName
        this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = environment.kafkaBootstrapServers
    }
}

fun createPersonFlagget84Producer(
    env: Environment,
): KafkaProducer<String, StatusEndring> {
    val kafkaProducerPropertes = kafkaPersonFlaggetProducerProperties(env)
    return KafkaProducer(kafkaProducerPropertes)
}