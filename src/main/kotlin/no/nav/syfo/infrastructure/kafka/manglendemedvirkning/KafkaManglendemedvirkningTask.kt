package no.nav.syfo.infrastructure.kafka.manglendemedvirkning

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.application.launchBackgroundTask
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.kafka.commonKafkaAivenConsumerConfig
import no.nav.syfo.objectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

const val TOPIC = "teamsykefravr.manglende-medvirkning-vurdering"
val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

fun launchKafkaTaskManglendeMedvirkning(
    applicationState: ApplicationState,
    environment: Environment,
    pengestoppService: PengestoppService,
) {
    launchBackgroundTask(
        applicationState = applicationState
    ) {
        log.info("Setting up kafka consumer for ${ManglendeMedvirkningVurderingRecord::class.java.simpleName}")

        val kafkaConsumer = KafkaConsumer<String, ManglendeMedvirkningVurderingRecord>(
            kafkaConsumerConfig(kafkaEnvironment = environment.kafka)
        )
        val manglendeMedvirkningVurderingConsumer = ManglendeMedvirkningVurderingConsumer(
            pengestoppService = pengestoppService,
            kafkaConsumer = kafkaConsumer,
        )

        while (applicationState.ready) {
            if (kafkaConsumer.subscription().isEmpty()) {
                kafkaConsumer.subscribe(listOf(TOPIC))
            }
            manglendeMedvirkningVurderingConsumer.pollAndProcessRecords()
        }
    }
}

private fun kafkaConsumerConfig(
    kafkaEnvironment: KafkaEnvironment,
): Properties = Properties().apply {
    putAll(commonKafkaAivenConsumerConfig(kafkaEnvironment))
    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ManglendeMedvirkningVurderingRecordDeserializer::class.java.canonicalName
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
}

class ManglendeMedvirkningVurderingRecordDeserializer : Deserializer<ManglendeMedvirkningVurderingRecord> {
    override fun deserialize(topic: String, data: ByteArray): ManglendeMedvirkningVurderingRecord =
        objectMapper.readValue(data, ManglendeMedvirkningVurderingRecord::class.java)
}
