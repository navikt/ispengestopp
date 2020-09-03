/*package no.nav.syfo.api

import kotlinx.coroutines.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.kafka.JacksonKafkaSerializer
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

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

    val prodConsumerProperties = baseConfig
            .toConsumerConfig("prodConsumer", valueDeserializer = StringDeserializer::class)

    val testProducerProperties = baseConfig.toProducerConfig("spek.integration-producer", JacksonKafkaSerializer::class)
    val testProducer = KafkaProducer<String, StatusEndring>(testProducerProperties)


    describe("Handle errors when storing kafka event to database") {
        val mockDatabase by lazy { TestDB() }
//        every { mockDatabase.addStatus(sykmeldtFnr, veilederIdent, enhetNr, primaryJob)  } throws Exception()

        val prodConsumer = KafkaConsumer<String, String>(prodConsumerProperties)
        prodConsumer.subscribe(listOf(env.stoppAutomatikkTopic))


        applicationState.ready.set(true)
        val kHendelse = StatusEndring(
                veilederIdent,
                sykmeldtFnr,
                Status.STOPP_AUTOMATIKK,
                primaryJob,
                OffsetDateTime.now(ZoneOffset.UTC),
                enhetNr
        )

        testProducer.send(
                ProducerRecord(
                        env.stoppAutomatikkTopic,
                        "123",
                        kHendelse
                )
        )

        it("Kafka queue is empty") {
            runBlocking {
                val job = launch(Dispatchers.Default) {
                    println("i launch")
                    println("applicationState: ${applicationState.ready.get()}")
                    blockingApplicationLogic(applicationState, mockDatabase, prodConsumer)

                    delay(2000)
                    println("set app state to false")
                    applicationState.ready.set(false)
                }

                job.cancelAndJoin()
                println("cancel and join")

                /*
                val messages = mutableListOf<StatusEndring>()
                prodConsumer.poll(Duration.ofMillis(5000)).forEach {
                    val hendelse: StatusEndring =
                            objectMapper.readValue(it.value())
                    messages.add(hendelse)
                }
                println("messages ${messages.size}")
                messages.size shouldBeEqualTo 0
                */

            }
        }

        it("Exception thrown from mockdatabase") {
//            verify { mockDatabase.addStatus(sykmeldtFnr, veilederIdent, enhetNr, primaryJob) }
//            confirmVerified(mockDatabase)
        }

    }

})
*/
