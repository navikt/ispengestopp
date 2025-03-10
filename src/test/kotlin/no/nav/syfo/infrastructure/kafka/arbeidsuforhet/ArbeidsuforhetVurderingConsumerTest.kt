package no.nav.syfo.infrastructure.kafka.arbeidsuforhet

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
import no.nav.syfo.testutils.generator.generateArbeidsuforhetVurdering
import no.nav.syfo.testutils.mockRecords
import no.nav.syfo.testutils.testEnvironment
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Future
import kotlin.test.assertEquals

class ArbeidsuforhetVurderingConsumerTest {

    val database = TestDB()
    val env = testEnvironment()

    val kafkaProducer = mockk<KafkaProducer<String, StatusEndring>>()
    val kafkaConsumer = mockk<KafkaConsumer<String, ArbeidsuforhetVurderingRecord>>()

    val repository = PengestoppRepository(database = database)
    val statusEndringProducer = StatusEndringProducer(
        environment = env,
        kafkaProducer = kafkaProducer
    )
    val pengestoppService = PengestoppService(pengestoppRepository = repository, statusEndringProducer = statusEndringProducer)

    val arbeidsuforhetVurderingConsumer = ArbeidsuforhetVurderingConsumer(
        pengestoppService = pengestoppService,
        kafkaConsumer = kafkaConsumer
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { kafkaConsumer.commitSync() } returns Unit
        every { kafkaProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        database.dropData()
    }

    @Test
    fun `creates statusendring and publish to kafka when arbeidsuforhetvurdering is AVSLAG`() {
        val vurdering = generateArbeidsuforhetVurdering(VurderingType.AVSLAG)
        val records = mockRecords(listOf(vurdering))
        every { kafkaConsumer.poll(any<Duration>()) } returns records

        arbeidsuforhetVurderingConsumer.pollAndProcessRecords()

        val statusEndring = repository.getStatusEndringer(personIdent = PersonIdent(vurdering.personident)).firstOrNull()
        assertEquals(statusEndring?.arsakList?.get(0)?.type, SykepengestoppArsak.MEDISINSK_VILKAR)
        assertEquals(statusEndring?.sykmeldtFnr?.value, vurdering.personident)
        assertEquals(statusEndring?.status, Status.STOPP_AUTOMATIKK)
        assertEquals(statusEndring?.veilederIdent?.value, vurdering.veilederident)

        verify(exactly = 1) { kafkaProducer.send(any()) }
        verify(exactly = 1) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `creates several statusendinger when multiple arbeidsuforhetvurderinger are AVSLAG`() {
        val vurdering1 = generateArbeidsuforhetVurdering(VurderingType.AVSLAG)
        val vurdering2 = generateArbeidsuforhetVurdering(VurderingType.AVSLAG)
        val records = mockRecords(listOf(vurdering1, vurdering2))
        every { kafkaConsumer.poll(any<Duration>()) } returns records

        arbeidsuforhetVurderingConsumer.pollAndProcessRecords()

        val statusEndringer = repository.getStatusEndringer(personIdent = PersonIdent(vurdering1.personident))
        assertEquals(statusEndringer.size, 2)

        verify(exactly = 2) { kafkaProducer.send(any()) }
        verify(exactly = 1) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `does not create statusendring nor publish to kafka when arbeidsuforhetvurdering is not AVSLAG`() {
        val vurdering = generateArbeidsuforhetVurdering(VurderingType.OPPFYLT)
        val records = mockRecords(listOf(vurdering))
        every { kafkaConsumer.poll(any<Duration>()) } returns records

        arbeidsuforhetVurderingConsumer.pollAndProcessRecords()

        val statusEndring = repository.getStatusEndringer(personIdent = PersonIdent(vurdering.personident)).firstOrNull()
        assertEquals(statusEndring, null)

        verify(exactly = 0) { kafkaProducer.send(any()) }
        verify(exactly = 1) { kafkaConsumer.commitSync() }
    }
}
