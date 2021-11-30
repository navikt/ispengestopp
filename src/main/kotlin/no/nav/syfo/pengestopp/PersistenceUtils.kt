package no.nav.syfo.pengestopp

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.*
import no.nav.syfo.application.Environment
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.pengestopp.database.addStatus
import no.nav.syfo.pengestopp.database.getActiveFlags
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

fun pollAndPersist(consumer: KafkaConsumer<String, String>, database: DatabaseInterface, env: Environment) {
    consumer.poll(Duration.ofMillis(env.pollTimeOutMs)).forEach { consumerRecord ->
        val hendelse: StatusEndring = objectMapper.readValue(consumerRecord.value())
        log.info("Offset for topic: ${env.stoppAutomatikkTopic}, offset: ${consumerRecord.offset()}")
        try {
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
                log.error("Record with uuid=${hendelse.uuid} is already stored and is skipped")
                COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED.inc()
            }
        } catch (e: Exception) {
            // TODO: Legg på retry kø
            COUNT_ENDRE_PERSON_STATUS_DB_FAILED.inc()
            log.error("Klarte ikke lagre til database. Hopper over melding. Feilet pga: ${e.javaClass}")
        }
    }
}
