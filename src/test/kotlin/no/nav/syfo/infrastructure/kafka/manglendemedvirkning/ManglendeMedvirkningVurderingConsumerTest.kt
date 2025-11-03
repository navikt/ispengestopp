package no.nav.syfo.infrastructure.kafka.manglendemedvirkning

import io.mockk.*
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.StatusEndringProducer
import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.generator.genereateManglendeMedvirkningVurdering
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Future

class ManglendeMedvirkningVurderingConsumerTest {
    private val database = TestDB()
    private val env = testEnvironment()

    private val kafkaProducer = mockk<KafkaProducer<String, StatusEndring>>()
    private val kafkaConsumer = mockk<KafkaConsumer<String, ManglendeMedvirkningVurderingRecord>>()

    private val repository = PengestoppRepository(database = database)
    private val statusEndringProducer = StatusEndringProducer(
        environment = env,
        kafkaProducer = kafkaProducer
    )
    private val pengestoppService = PengestoppService(pengestoppRepository = repository, statusEndringProducer = statusEndringProducer)

    private val manglendeMedvirkningVurderingConsumer = ManglendeMedvirkningVurderingConsumer(
        pengestoppService = pengestoppService,
        kafkaConsumer = kafkaConsumer
    )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        every { kafkaConsumer.commitSync() } returns Unit
        coEvery { kafkaProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        database.dropData()
    }

    @Test
    fun `creates statusendring when STANS`() {
        val records = mockRecords(listOf(genereateManglendeMedvirkningVurdering(VurderingType.STANS)))

        every { kafkaConsumer.poll(any<Duration>()) } returns records

        manglendeMedvirkningVurderingConsumer.pollAndProcessRecords()

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        verify(exactly = 1) { kafkaProducer.send(any()) }

        val statusEndringer = repository.getStatusEndringer(UserConstants.SYKMELDT_PERSONIDENT)
        assertEquals(1, statusEndringer.size)
        val statusEndring = statusEndringer.first()
        assertEquals(UserConstants.SYKMELDT_PERSONIDENT, statusEndring.sykmeldtFnr)
        assertEquals(VeilederIdent("Z999999"), statusEndring.veilederIdent)
        assertEquals(Status.STOPP_AUTOMATIKK, statusEndring.status)
        assertEquals(listOf(Arsak(SykepengestoppArsak.MANGLENDE_MEDVIRKING)), statusEndring.arsakList)
        assertNull(statusEndring.virksomhetNr)
        assertNull(statusEndring.enhetNr)
    }

    @Test
    fun `creates no statusendring for other vurdering types`() {
        val manglendeMedvirkningVurderingRecords = VurderingType.entries.filter { it != VurderingType.STANS }.map { genereateManglendeMedvirkningVurdering(it) }
        val records = mockRecords(manglendeMedvirkningVurderingRecords)

        every { kafkaConsumer.poll(any<Duration>()) } returns records

        manglendeMedvirkningVurderingConsumer.pollAndProcessRecords()

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        verify(exactly = 0) { kafkaProducer.send(any()) }

        val statusEndringer = repository.getStatusEndringer(UserConstants.SYKMELDT_PERSONIDENT)
        assertTrue(statusEndringer.isEmpty())
    }
}
