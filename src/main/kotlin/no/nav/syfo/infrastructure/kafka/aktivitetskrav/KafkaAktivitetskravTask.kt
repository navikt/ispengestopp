package no.nav.syfo.infrastructure.kafka.aktivitetskrav

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

const val TOPIC = "teamsykefravr.aktivitetskrav-vurdering"
val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

fun launchKafkaTaskAktivitetskrav(
    applicationState: ApplicationState,
    environment: Environment,
    pengestoppService: PengestoppService,
) {
    launchBackgroundTask(
        applicationState = applicationState
    ) {
        log.info("Setting up kafka consumer for ${AktivitetskravVurderingRecord::class.java.simpleName}")

        val kafkaConsumer = KafkaConsumer<String, AktivitetskravVurderingRecord>(
            kafkaConsumerConfig(kafkaEnvironment = environment.kafka)
        )
        val aktivitetskravVurderingConsumer = AktivitetskravVurderingConsumer(
            pengestoppService = pengestoppService,
            kafkaConsumer = kafkaConsumer,
        )

        while (applicationState.ready) {
            if (kafkaConsumer.subscription().isEmpty()) {
                kafkaConsumer.subscribe(listOf(TOPIC))
            }
            aktivitetskravVurderingConsumer.pollAndProcessRecords()
        }
    }
}

private fun kafkaConsumerConfig(
    kafkaEnvironment: KafkaEnvironment,
): Properties = Properties().apply {
    putAll(commonKafkaAivenConsumerConfig(kafkaEnvironment))
    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = AktivitetskravVurderingRecordDeserializer::class.java.canonicalName
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
}

class AktivitetskravVurderingRecordDeserializer : Deserializer<AktivitetskravVurderingRecord> {
    override fun deserialize(topic: String, data: ByteArray): AktivitetskravVurderingRecord =
        objectMapper.readValue(data, AktivitetskravVurderingRecord::class.java)
}
