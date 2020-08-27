package no.nav.syfo.kafka

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

fun createPersonFlagget84Consumer(
    env: Environment,
    vaultSecrets: VaultSecrets
): KafkaConsumer<String, String> {
    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
    val properties = kafkaBaseConfig.toConsumerConfig(
        "${env.applicationName}-consumer",
        valueDeserializer = StringDeserializer::class
    )

    val personFlagget84Consumer = KafkaConsumer<String, String>(properties)
    personFlagget84Consumer.subscribe(listOf(env.stoppAutomatikkTopic))

    return personFlagget84Consumer
}
