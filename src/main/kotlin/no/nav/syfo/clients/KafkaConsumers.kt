package no.nav.syfo.clients

import no.nav.syfo.Environment
import no.nav.syfo.kafka.KafkaCredentials
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

class KafkaConsumers(env: Environment, vaultSecrets: KafkaCredentials) {
    private val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
    private val properties = kafkaBaseConfig.toConsumerConfig(
        "${env.applicationName}-consumer2",
        valueDeserializer = StringDeserializer::class
    )

    private fun Properties.addExtraProps(): Properties {
        this.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        return this
    }

    val kafkaConsumerSmReg = KafkaConsumer<String, String>(properties.addExtraProps())
}
