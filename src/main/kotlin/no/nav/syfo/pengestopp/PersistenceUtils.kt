package no.nav.syfo.pengestopp

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.application.Environment
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.objectMapper
import no.nav.syfo.pengestopp.database.addStatus
import no.nav.syfo.pengestopp.database.getActiveFlags
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

fun pollAndPersist(
    consumer: KafkaConsumer<String, String>,
    database: DatabaseInterface,
    env: Environment,
) {
    val records = consumer.poll(Duration.ofMillis(env.pollTimeOutMs))
    if (!records.isEmpty) {
        records.forEach { consumerRecord ->
            val hendelse: StatusEndring = objectMapper.readValue(consumerRecord.value())
            val statusEndringList = database.getActiveFlags(UUID.fromString(hendelse.uuid))

            if (statusEndringList.isEmpty()) {
                database.addStatus(
                    hendelse.uuid,
                    hendelse.sykmeldtFnr,
                    hendelse.veilederIdent,
                    hendelse.enhetNr,
                    hendelse.arsakList,
                    hendelse.virksomhetNr,
                    hendelse.opprettet,
                )
            } else {
                log.warn("Record with uuid=${hendelse.uuid} is already stored and is skipped")
                COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED.increment()
            }
        }
        if (env.useAivenTopic) {
            consumer.commitSync()
        }
    }
}
