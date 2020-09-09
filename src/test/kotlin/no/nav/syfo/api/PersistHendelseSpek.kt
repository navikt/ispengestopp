package no.nav.syfo.api

import no.nav.syfo.api.testutils.hentStatusEndringListe
import org.amshove.kluent.shouldBeEqualTo

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.application.ApplicationState
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.apache.kafka.common.TopicPartition
import java.time.*

object PersistHendelseSpek : Spek({
    var applicationState = ApplicationState()
    val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        topicNames = listOf("apen-isyfo-stoppautomatikk")
    )

    val sykmeldtFnr = SykmeldtFnr("123456")
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")
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
        "apen-isyfo-stoppautomatikk",
        0L
    )

    val partition = 0
    val stoppAutomatikkTopicPartition = TopicPartition(env.stoppAutomatikkTopic, partition)
    val hendelse =
        "{\"veilederIdent\":{\"value\":\"Z999999\"},\"sykmeldtFnr\":{\"value\":\"123456\"}," +
                "\"status\":\"STOPP_AUTOMATIKK\",\"virksomhetNr\":{\"value\":\"888\"}," +
                "\"opprettet\":\"2020-09-09T12:12:27.1248441Z\",\"enhetNr\":{\"value\":\"9999\"}}"
    val hendelseRecord = ConsumerRecord(env.stoppAutomatikkTopic, partition, 1, "something", hendelse )

    describe("Handle errors when storing kafka event to database") {
        val database = TestDB()

        afterGroup {
            database.stop()
        }


        val mockConsumer = mockk<KafkaConsumer<String, String>>()
        every { mockConsumer.poll(Duration.ZERO) } returns ConsumerRecords(
            mapOf(stoppAutomatikkTopicPartition to listOf(hendelseRecord))) andThen ConsumerRecords.empty()

        applicationState.ready.set(true)

        it("Catch thrown exception") {
            runBlockingTest {
                try {
                    withTimeout(1000L) { // Cancel coroutine after a certain time
                        blockingApplicationLogic(applicationState, database, mockConsumer, env)
                    }
                } catch (e: CancellationException) {
                    println("Cancellation exception for suspended thread")
                }
            }
            var statusendringListe: List<StatusEndring> = database.connection.hentStatusEndringListe(sykmeldtFnr, primaryJob)
            statusendringListe.size shouldBeEqualTo 1

            val statusEndring = statusendringListe[0]
            statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
            statusEndring.veilederIdent shouldBeEqualTo veilederIdent
            statusEndring.virksomhetNr shouldBeEqualTo primaryJob
            statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
            statusEndring.opprettet.dayOfMonth shouldBeEqualTo
                    Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime().dayOfMonth
            statusEndring.enhetNr shouldBeEqualTo enhetNr

        }
    }

})
