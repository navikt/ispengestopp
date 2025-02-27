package no.nav.syfo.infrastructure.kafka
import no.nav.syfo.application.Environment
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.StatusEndring
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.*

class StatusEndringProducer(
    private val environment: Environment,
    private val kafkaProducer: KafkaProducer<String, StatusEndring> = createPersonFlagget84AivenProducer(kafkaEnvironment = environment.kafka)
) {
    private val topic = environment.stoppAutomatikkAivenTopic

    fun send(statusEndring: StatusEndring) {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                statusEndring.sykmeldtFnr.asProducerRecordKey(),
                statusEndring,
            )
        )
        log.info("Lagt melding på kafka: topic: $topic")
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

fun PersonIdent.asProducerRecordKey(): String = UUID.nameUUIDFromBytes(value.toByteArray()).toString()

private fun createPersonFlagget84AivenProducer(kafkaEnvironment: KafkaEnvironment): KafkaProducer<String, StatusEndring> {
    val kafkaProducerProperties = Properties().apply {
        putAll(commonKafkaAivenConfig(kafkaEnvironment = kafkaEnvironment))
        this[ProducerConfig.ACKS_CONFIG] = "all"
        this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        this[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "1"
        this[ProducerConfig.MAX_BLOCK_MS_CONFIG] = "15000"
        this[ProducerConfig.RETRIES_CONFIG] = "100000"
        this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.canonicalName
        this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java.canonicalName
    }

    return KafkaProducer(kafkaProducerProperties)
}
