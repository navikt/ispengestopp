package no.nav.syfo.infrastructure.kafka.manglendemedvirkning

import no.nav.syfo.application.PengestoppService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class ManglendeMedvirkningVurderingConsumer(private val pengestoppService: PengestoppService, private val kafkaConsumer: KafkaConsumer<String, ManglendeMedvirkningVurderingRecord>) {
    fun pollAndProcessRecords() {
        val records = kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SECONDS))
        if (records.count() > 0) {
            val stansVurderinger =
                records.mapNotNull { it.value() }
                    .filter { it.vurderingType.value == VurderingType.STANS }
            val statusEndringer = stansVurderinger.map {
                StatusEndring(
                    uuid = UUID.randomUUID().toString(),
                    veilederIdent = VeilederIdent(it.veilederident),
                    sykmeldtFnr = PersonIdent(it.personident),
                    status = Status.STOPP_AUTOMATIKK,
                    arsakList = listOf(Arsak(SykepengestoppArsak.MANGLENDE_MEDVIRKING)),
                    virksomhetNr = null,
                    opprettet = OffsetDateTime.now(ZoneOffset.UTC),
                    enhetNr = null
                )
            }
            if (statusEndringer.isNotEmpty()) {
                pengestoppService.createStatusendringer(statusEndringer)
                log.info("ManglendeMedvirkningVurderingConsumer created ${statusEndringer.size} statusendringer")
            }
            kafkaConsumer.commitSync()
        }
    }

    companion object {
        private const val POLL_DURATION_SECONDS = 10L
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
