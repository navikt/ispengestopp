package no.nav.syfo.pengestopp.kafka

import no.nav.syfo.application.Environment
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun kafkaPersonFlaggetConsumerProperties(
    environment: Environment,
) = Properties().apply {
    this[ConsumerConfig.GROUP_ID_CONFIG] = "${environment.applicationName}-consumer-gcp-v1"
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    this[CommonClientConfigs.RETRIES_CONFIG] = "2"
    this["acks"] = "all"
    this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
    this[SaslConfigs.SASL_MECHANISM] = "PLAIN"
    this["schema.registry.url"] = environment.kafkaSchemaRegistryUrl
    this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName

    this[SaslConfigs.SASL_JAAS_CONFIG] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"${environment.serviceuserUsername}\" password=\"${environment.serviceuserPassword}\";"
    this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = environment.kafkaBootstrapServers
}

fun createPersonFlagget84Consumer(
    env: Environment,
    groupId: String? = null,
): KafkaConsumer<String, String> {
    val kafkaConsumerProperties = kafkaPersonFlaggetConsumerProperties(env)
    if (groupId != null) {
        kafkaConsumerProperties[ConsumerConfig.GROUP_ID_CONFIG] = groupId
    }

    return KafkaConsumer<String, String>(kafkaConsumerProperties).also {
        it.subscribe(listOf(env.stoppAutomatikkTopic))
    }
}
