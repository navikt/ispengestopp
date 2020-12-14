package no.nav.syfo.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.*
import no.nav.syfo.database.DatabaseInterface
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

fun pollAndPersist(consumer: KafkaConsumer<String, String>, database: DatabaseInterface, env: Environment) {
    consumer.poll(Duration.ofMillis(env.pollTimeOutMs)).forEach { consumerRecord ->
        val hendelse: StatusEndring = objectMapper.readValue(consumerRecord.value())
        log.info("Offset for topic: ${env.stoppAutomatikkTopic}, offset: ${consumerRecord.offset()}")
        try {
            val statusEndringList = hendelse.uuid?.let {
                database.getActiveFlags(UUID.fromString(hendelse.uuid))
            } ?: emptyList()
            if (statusEndringList.isEmpty()) {
                database.addStatus(
                    hendelse.uuid ?: UUID.randomUUID().toString(),
                    hendelse.sykmeldtFnr,
                    hendelse.veilederIdent,
                    hendelse.enhetNr,
                    hendelse.virksomhetNr
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
