package no.nav.syfo.infrastructure.kafka.aktivitetskrav

import io.mockk.*
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.StatusEndringProducer
import no.nav.syfo.pengestopp.Status
import no.nav.syfo.pengestopp.StatusEndring
import no.nav.syfo.pengestopp.SykepengestoppArsak
import no.nav.syfo.testutils.TestDB
import no.nav.syfo.testutils.dropData
import no.nav.syfo.testutils.generator.generateAktivitetskravVurdering
import no.nav.syfo.testutils.mockRecords
import no.nav.syfo.testutils.testEnvironment
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.Future
import kotlin.test.assertEquals

class AktivitetskravVurderingConsumerTest {

    val database = TestDB()
    val env = testEnvironment()

    val kafkaProducer = mockk<KafkaProducer<String, StatusEndring>>()
    val kafkaConsumer = mockk<KafkaConsumer<String, AktivitetskravVurderingRecord>>()

    val repository = PengestoppRepository(database = database)
    val statusEndringProducer = StatusEndringProducer(
        environment = env,
        kafkaProducer = kafkaProducer,
    )
    val pengestoppService = PengestoppService(
        pengestoppRepository = repository,
        statusEndringProducer = statusEndringProducer,
    )

    val aktivitetskravVurderingConsumer = AktivitetskravVurderingConsumer(
        pengestoppService = pengestoppService,
        kafkaConsumer = kafkaConsumer,
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { kafkaConsumer.commitSync() } returns Unit
        every { kafkaProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        database.dropData()
    }

    @Test
    fun `creates statusendring and publish to kafka when aktivitetskravvurdering is INNSTILLING_OM_STANS`() {
        val vurdering = generateAktivitetskravVurdering(AktivitetskravStatus.INNSTILLING_OM_STANS)
        val records = mockRecords(listOf(vurdering))
        every { kafkaConsumer.poll(any<Duration>()) } returns records

        aktivitetskravVurderingConsumer.pollAndProcessRecords()

        val statusEndring = repository.getStatusEndringer(personIdent = PersonIdent(vurdering.personIdent)).firstOrNull()
        assertEquals(statusEndring?.arsakList?.get(0)?.type, SykepengestoppArsak.AKTIVITETSKRAV)
        assertEquals(statusEndring?.sykmeldtFnr?.value, vurdering.personIdent)
        assertEquals(statusEndring?.status, Status.STOPP_AUTOMATIKK)
        assertEquals(statusEndring?.veilederIdent?.value, vurdering.updatedBy)

        verify(exactly = 1) { kafkaProducer.send(any()) }
        verify(exactly = 1) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `creates several statusendinger when multiple aktivitetskravvurderinger are INNSTILLING_OM_STANS`() {
        val vurdering1 = generateAktivitetskravVurdering(AktivitetskravStatus.INNSTILLING_OM_STANS)
        val vurdering2 = generateAktivitetskravVurdering(AktivitetskravStatus.INNSTILLING_OM_STANS)
        val records = mockRecords(listOf(vurdering1, vurdering2))
        every { kafkaConsumer.poll(any<Duration>()) } returns records

        aktivitetskravVurderingConsumer.pollAndProcessRecords()

        val statusEndringer = repository.getStatusEndringer(personIdent = PersonIdent(vurdering1.personIdent))
        assertEquals(statusEndringer.size, 2)

        verify(exactly = 2) { kafkaProducer.send(any()) }
        verify(exactly = 1) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `does not create statusendring nor publish to kafka when aktivitetskravvurdering is not INNSTILLING_OM_STANS`() {
        val vurdering = generateAktivitetskravVurdering(AktivitetskravStatus.UNNTAK)
        val records = mockRecords(listOf(vurdering))
        every { kafkaConsumer.poll(any<Duration>()) } returns records

        aktivitetskravVurderingConsumer.pollAndProcessRecords()

        val statusEndring = repository.getStatusEndringer(personIdent = PersonIdent(vurdering.personIdent)).firstOrNull()
        assertEquals(statusEndring, null)

        verify(exactly = 0) { kafkaProducer.send(any()) }
        verify(exactly = 1) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `throws error when aktivitetskravvurdering is missing veilederIdent`() {
        val vurdering = generateAktivitetskravVurdering(AktivitetskravStatus.INNSTILLING_OM_STANS).copy(updatedBy = null)
        val records = mockRecords(listOf(vurdering))
        every { kafkaConsumer.poll(any<Duration>()) } returns records

        assertThrows<IllegalStateException> {
            aktivitetskravVurderingConsumer.pollAndProcessRecords()
        }

        verify(exactly = 0) { kafkaProducer.send(any()) }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }
}
