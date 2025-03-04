package no.nav.syfo.infrastructure.kafka.arbeidsuforhet

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.StatusEndringProducer
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.ManglendeMedvirkningVurderingConsumer
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.ManglendeMedvirkningVurderingRecord
import no.nav.syfo.pengestopp.StatusEndring
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
import java.util.concurrent.Future

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
        coEvery { kafkaProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        database.dropData()
    }
    
    @Test
    fun `creates statusendring and publish to kafka when arbeidsuforhetvurdering is AVSLAG`() {
        val records = mockRecords(listOf(generateArbeidsuforhetVurdering(VurderingType.AVSLAG)))
    }
}