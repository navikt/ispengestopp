package no.nav.syfo.kafka

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun kafkaPersonFlaggetConsumerProperties(
    environment: Environment,
    vaultSecrets: VaultSecrets
) = Properties().apply {
    this[ConsumerConfig.GROUP_ID_CONFIG] = "${environment.applicationName}-consumer"
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none"
    this[CommonClientConfigs.RETRIES_CONFIG] = "2"
    this["acks"] = "all"
    this["security.protocol"] = "SASL_SSL"
    this["sasl.mechanism"] = "PLAIN"
    this["schema.registry.url"] = "http://kafka-schema-registry.tpa:8081"
    this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName

    this["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"${vaultSecrets.serviceuserUsername}\" password=\"${vaultSecrets.serviceuserPassword}\";"
    this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = environment.kafkaBootstrapServers
}

fun createPersonFlagget84Consumer(
    env: Environment,
    vaultSecrets: VaultSecrets
): KafkaConsumer<String, String> {
    val kafkaConsumerProperties = kafkaPersonFlaggetConsumerProperties(env, vaultSecrets)

    val personFlagget84Consumer = KafkaConsumer<String, String>(kafkaConsumerProperties)
    personFlagget84Consumer.subscribe(listOf(env.stoppAutomatikkTopic))

    return personFlagget84Consumer
}
