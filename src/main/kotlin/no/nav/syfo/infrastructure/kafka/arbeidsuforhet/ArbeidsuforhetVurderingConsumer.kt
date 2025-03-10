package no.nav.syfo.infrastructure.kafka.arbeidsuforhet

import no.nav.syfo.application.PengestoppService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class ArbeidsuforhetVurderingConsumer(
    private val pengestoppService: PengestoppService,
    private val kafkaConsumer: KafkaConsumer<String, ArbeidsuforhetVurderingRecord>
) {
    fun pollAndProcessRecords() {
        val records = kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SECONDS))
        if (records.count() > 0) {
            val avslagVurderinger = records.mapNotNull { it.value() }.filter { it.type == VurderingType.AVSLAG }
            if (avslagVurderinger.isNotEmpty()) {
                val statusEndringer = avslagVurderinger.map {
                    StatusEndring(
                        uuid = UUID.randomUUID().toString(),
                        veilederIdent = VeilederIdent(it.veilederident),
                        sykmeldtFnr = PersonIdent(it.personident),
                        status = Status.STOPP_AUTOMATIKK,
                        arsakList = listOf(Arsak(SykepengestoppArsak.MEDISINSK_VILKAR)),
                        virksomhetNr = null,
                        opprettet = OffsetDateTime.now(ZoneOffset.UTC),
                        enhetNr = null,
                    )
                }

                pengestoppService.createStatusendringer(statusEndringer)
                log.info("ArbeidsuforhetVurderingConsumer created ${statusEndringer.size} statusendringer")
            }
        }
        kafkaConsumer.commitSync()
    }

    companion object {
        private const val POLL_DURATION_SECONDS = 10L
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
