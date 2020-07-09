package no.nav.syfo.kafka

import no.nav.syfo.Environment
import no.nav.syfo.KFlaggperson84Hendelse
import no.nav.syfo.VaultSecrets
import org.apache.kafka.clients.producer.KafkaProducer

fun createPersonFlagget84Producer(
    env: Environment,
    vaultSecrets: VaultSecrets
): KafkaProducer<String, KFlaggperson84Hendelse> {
    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
    val properties = kafkaBaseConfig.toProducerConfig(
        env.applicationName,
        GsonKafkaSerializer::class
    )

    return KafkaProducer(properties)
}
