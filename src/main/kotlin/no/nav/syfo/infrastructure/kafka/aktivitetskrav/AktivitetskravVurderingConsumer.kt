package no.nav.syfo.infrastructure.kafka.aktivitetskrav

import no.nav.syfo.application.PengestoppService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class AktivitetskravVurderingConsumer(
    private val pengestoppService: PengestoppService,
    private val kafkaConsumer: KafkaConsumer<String, AktivitetskravVurderingRecord>,
) {
    fun pollAndProcessRecords() {
        val records = kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SECONDS))
        if (records.count() > 0) {
            val stansVurderinger = records.mapNotNull { it.value() }.filter { it.status == AktivitetskravStatus.INNSTILLING_OM_STANS.name }
            if (stansVurderinger.isNotEmpty()) {
                val statusEndringer = stansVurderinger.map { vurdering ->
                    StatusEndring(
                        uuid = UUID.randomUUID().toString(),
                        veilederIdent = vurdering.updatedBy?.let { VeilederIdent(it) }
                            ?: throw IllegalStateException("Stans-vurderingen mangler veilederIdent og kan ikke lagres"),
                        sykmeldtFnr = PersonIdent(vurdering.personIdent),
                        status = Status.STOPP_AUTOMATIKK,
                        arsakList = listOf(Arsak(SykepengestoppArsak.AKTIVITETSKRAV)),
                        virksomhetNr = null,
                        opprettet = OffsetDateTime.now(ZoneOffset.UTC),
                        enhetNr = null,
                    )
                }

                pengestoppService.createStatusendringer(statusEndringer)
                log.info("AktivitetskravVurderingConsumer created ${statusEndringer.size} statusendringer")
            }
        }
        kafkaConsumer.commitSync()
    }

    companion object {
        private const val POLL_DURATION_SECONDS = 10L
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
