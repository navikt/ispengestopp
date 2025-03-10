package no.nav.syfo.infrastructure.kafka.manglendemedvirkning

import io.mockk.*
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.StatusEndringProducer
import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.TestDB
import no.nav.syfo.testutils.UserConstants
import no.nav.syfo.testutils.dropData
import no.nav.syfo.testutils.generator.genereateManglendeMedvirkningVurdering
import no.nav.syfo.testutils.testEnvironment
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.util.concurrent.Future

class ManglendeMedvirkningVurderingConsumerSpek : Spek({
    val database = TestDB()
    val env = testEnvironment()

    val kafkaProducer = mockk<KafkaProducer<String, StatusEndring>>()
    val kafkaConsumer = mockk<KafkaConsumer<String, ManglendeMedvirkningVurderingRecord>>()

    val repository = PengestoppRepository(database = database)
    val statusEndringProducer = StatusEndringProducer(
        environment = env,
        kafkaProducer = kafkaProducer
    )
    val pengestoppService = PengestoppService(pengestoppRepository = repository, statusEndringProducer = statusEndringProducer)

    val manglendeMedvirkningVurderingConsumer = ManglendeMedvirkningVurderingConsumer(
        pengestoppService = pengestoppService,
        kafkaConsumer = kafkaConsumer
    )

    beforeEachTest {
        clearAllMocks()
        every { kafkaConsumer.commitSync() } returns Unit
        coEvery { kafkaProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    afterEachTest {
        database.connection.use { it.dropData() }
    }

    describe("pollAndProcessRecords") {
        it("creates statusendring when STANS") {
            val records = mockRecords(listOf(genereateManglendeMedvirkningVurdering(VurderingType.STANS)))

            every { kafkaConsumer.poll(any<Duration>()) } returns records

            manglendeMedvirkningVurderingConsumer.pollAndProcessRecords()

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }

            verify(exactly = 1) { kafkaProducer.send(any()) }

            val statusEndringer = repository.getStatusEndringer(UserConstants.SYKMELDT_PERSONIDENT)
            statusEndringer.size shouldBeEqualTo 1
            val statusEndring = statusEndringer.first()
            statusEndring.sykmeldtFnr shouldBeEqualTo UserConstants.SYKMELDT_PERSONIDENT
            statusEndring.veilederIdent shouldBeEqualTo VeilederIdent("Z999999")
            statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
            statusEndring.arsakList shouldBeEqualTo listOf(Arsak(SykepengestoppArsak.MANGLENDE_MEDVIRKING))
            statusEndring.virksomhetNr.shouldBeNull()
            statusEndring.enhetNr.shouldBeNull()
        }
        it("creates no statusendring for other vurdering types") {
            val manglendeMedvirkningVurderingRecords = VurderingType.entries.filter { it != VurderingType.STANS }.map { genereateManglendeMedvirkningVurdering(it) }
            val records = mockRecords(manglendeMedvirkningVurderingRecords)

            every { kafkaConsumer.poll(any<Duration>()) } returns records

            manglendeMedvirkningVurderingConsumer.pollAndProcessRecords()

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }

            verify(exactly = 0) { kafkaProducer.send(any()) }

            val statusEndringer = repository.getStatusEndringer(UserConstants.SYKMELDT_PERSONIDENT)
            statusEndringer.shouldBeEmpty()
        }
    }
})

private fun mockRecords(records: List<ManglendeMedvirkningVurderingRecord>): ConsumerRecords<String, ManglendeMedvirkningVurderingRecord> {
    val consumerRecords = records.mapIndexed { index, record ->
        ConsumerRecord("topic", 0, index.toLong(), "key$index", record)
    }
    return ConsumerRecords(mapOf(TopicPartition("topic", 0) to consumerRecords))
}
