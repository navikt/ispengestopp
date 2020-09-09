package no.nav.syfo.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.database.DatabaseInterface
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.sql.SQLException
import org.apache.kafka.common.TopicPartition
import java.time.Duration

object HandleFailingDbSpek : Spek({
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
        val mockDatabase = TestDB()

        afterGroup {
            mockDatabase.stop()
        }

        mockkStatic("no.nav.syfo.QueriesKt")
        every { any<DatabaseInterface>().addStatus(any(), any(), any(), any()) } throws SQLException("Sql er feil")

        val mockConsumer = mockk<KafkaConsumer<String, String>>()
        every { mockConsumer.poll(Duration.ZERO) } returns ConsumerRecords( mapOf(stoppAutomatikkTopicPartition to listOf(hendelseRecord)))

        applicationState.ready.set(true)

        it("Catch thrown exception") {
            runBlockingTest {
                try {
                    withTimeout(100L) { // Cancel coroutine after a certain time
                        blockingApplicationLogic(applicationState, mockDatabase, mockConsumer, env)
                    }
                } catch (e: CancellationException) {
                    println("Cancellation exception for suspended thread")
                }
            }
            verify { any<DatabaseInterface>().addStatus(any(), any(), any(), any()) }
        }
    }

})
