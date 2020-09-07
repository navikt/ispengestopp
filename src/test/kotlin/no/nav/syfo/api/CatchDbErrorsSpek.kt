package no.nav.syfo.api

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.kafka.JacksonKafkaSerializer
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.sql.SQLException
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
object CatchDbErrorsSpek : Spek({
    var applicationState = ApplicationState()
    val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        topicNames = listOf("apen-isyfo-stoppautomatikk")
    )
    val env = Environment(
        "ispengestopp",
        8080,
        embeddedKafkaEnvironment.brokersURL,
        "",
        "",
        "",
        "https://sts.issuer.net/myid",
        "src/test/resources/jwkset.json",
        false,
        "1234",
        "apen-isyfo-stoppautomatikk"
    )
    val credentials = VaultSecrets(
        "",
        ""
    )
    val sykmeldtFnr = SykmeldtFnr("123456")
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val baseConfig = loadBaseConfig(env, credentials).overrideForTest()
    val testProducerProperties =
        baseConfig.toProducerConfig("spek.unittest", valueSerializer = JacksonKafkaSerializer::class)
    val testProducer = KafkaProducer<String, StatusEndring>(testProducerProperties)

    val testConsumerProperties =
        baseConfig.toConsumerConfig("spek.unittest-testconsumer", valueDeserializer = StringDeserializer::class)
    val testConsumer = KafkaConsumer<String, String>(testConsumerProperties)

    describe("Handle errors when storing kafka event to database") {
        val mockDatabase = TestDB()

        mockkStatic("no.nav.syfo.QueriesKt")
        every { any<DatabaseInterface>().addStatus(any(), any(), any(), any()) } throws SQLException()

        applicationState.ready.set(true)
        val kHendelse = StatusEndring(
            veilederIdent,
            sykmeldtFnr,
            Status.STOPP_AUTOMATIKK,
            primaryJob,
            OffsetDateTime.now(ZoneOffset.UTC),
            enhetNr
        )

        embeddedKafkaEnvironment.start()
        testProducer.send(
            ProducerRecord(
                env.stoppAutomatikkTopic,
                kHendelse
            )
        )
        testConsumer.subscribe(listOf(env.stoppAutomatikkTopic))

        it("Catch thrown exception") {
            runBlockingTest {
                try {
                    withTimeout(500L) { // Cancel coroutine after a certain time
                        blockingApplicationLogic(applicationState, mockDatabase, testConsumer, env)
                    }
                } catch (e: CancellationException) {
                    println("Cancellation exception for suspended thread")
                }

            }

            verify { any<DatabaseInterface>().addStatus(any(), any(), any(), any()) }
            embeddedKafkaEnvironment.tearDown()
        }
    }

})

