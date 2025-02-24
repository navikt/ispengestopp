package no.nav.syfo.pengestopp

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.application.Environment
import no.nav.syfo.application.IPengestoppRepository
import no.nav.syfo.log
import no.nav.syfo.objectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

fun pollAndPersist(
    consumer: KafkaConsumer<String, String>,
    repository: IPengestoppRepository,
    env: Environment,
) {
    val records = consumer.poll(Duration.ofMillis(env.pollTimeOutMs))
    if (!records.isEmpty) {
        records.forEach { consumerRecord ->
            val statusEndring: StatusEndring = objectMapper.readValue(consumerRecord.value())
            val existingStatusEndring = repository.getStatusEndring(uuid = UUID.fromString(statusEndring.uuid))

            if (existingStatusEndring == null) {
                repository.createStatusEndring(statusEndring = statusEndring)
            } else {
                log.warn("Record with uuid=${statusEndring.uuid} is already stored and is skipped")
                COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED.increment()
            }
        }
        consumer.commitSync()
    }
}
