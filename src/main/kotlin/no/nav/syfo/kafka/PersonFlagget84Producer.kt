package no.nav.syfo.kafka

import no.nav.syfo.Environment
import no.nav.syfo.KFlaggperson84Hendelse
import no.nav.syfo.VaultSecrets
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer

fun createPersonFlagget84Producer(
    env: Environment,
    vaultSecrets: VaultSecrets
): KafkaProducer<String, KFlaggperson84Hendelse> {
    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
    val properties = kafkaBaseConfig.toProducerConfig(env.applicationName, StringSerializer::class) // TODO i smregistrering-backend brukes det her noe som setter egen security protocol for dev mode, jeg får ikke til serializer som denne krever (vi bør vel ha en Gson-versjon?)

    return KafkaProducer(properties)
}
