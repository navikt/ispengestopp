package no.nav.syfo.api
/*
// NOTE: This test runs successfully alone. However, we haven't been able to get the coroutines to consistently play
// nicely during testing. When we run all tests in succession this test makes the PostStatusSpek test check for results
// before the production code is done. Concurrency is hard.

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.api.testutils.generateJWT
import no.nav.syfo.api.testutils.mockSyfotilgangskontrollServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.setupAuth
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.kafka.JacksonKafkaSerializer
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths
import java.time.Duration
import java.util.*

@KtorExperimentalAPI
class HandleDBErrorSpek : Spek({
    val sykmeldtFnr = SykmeldtFnr("123456")
    val primaryJob = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")
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
    val applicationState = ApplicationState()

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val baseConfig = loadBaseConfig(env, credentials).overrideForTest()
    val testConsumerProperties = baseConfig
        .toConsumerConfig("spek.integration-consumer", valueDeserializer = StringDeserializer::class)
    val testConsumer = KafkaConsumer<String, String>(testConsumerProperties)
    testConsumer.subscribe(listOf(env.stoppAutomatikkTopic))

    val prodConsumerProperties = baseConfig
        .toConsumerConfig("prodConsumer", valueDeserializer = StringDeserializer::class)

    val producerProperties = baseConfig.toProducerConfig("spek.integration-producer", JacksonKafkaSerializer::class)
    val personFlagget84Producer = KafkaProducer<String, StatusEndring>(producerProperties)

    //TODO gjøre database delen av testen om til å gi mer test coverage av prodkoden
    fun withTestApplicationForApi(
        testApp: TestApplicationEngine,
        database: TestDB,
        block: TestApplicationEngine.() -> Unit
    ) {
        testApp.start()
        testApp.application.install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }

        val mockServerPort = 9092
        val mockHttpServerUrl = "http://localhost:$mockServerPort"

        val mockServer =
            mockSyfotilgangskontrollServer(mockServerPort, sykmeldtFnr).start(wait = false)

        afterGroup { mockServer.stop(1L, 10L) }

        val uri = Paths.get(env.jwksUri).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()

        testApp.application.setupAuth(env, jwkProvider)

        val prodConsumer = KafkaConsumer<String, String>(prodConsumerProperties)

        prodConsumer.subscribe(listOf(env.stoppAutomatikkTopic))
        applicationState.ready.set(true)

        launchListeners(
            applicationState,
            database,
            prodConsumer
        )

        testApp.application.routing {
            authenticate {
                registerFlaggPerson84(
                    database,
                    env,
                    personFlagget84Producer,
                    TilgangskontrollConsumer("$mockHttpServerUrl/syfo-tilgangskontroll/api/tilgang/bruker")
                )
            }
        }

        return testApp.block()
    }

    describe("Fail persisting to database") {
        val testDatabase = TestDB()
        mockkStatic("no.nav.syfo.QueriesKt")
        every { any<DatabaseInterface>().addStatus(any(), any(), any(), any()) } throws Exception()

        beforeGroup {
            embeddedKafkaEnvironment.start()
        }
        afterGroup {
            embeddedKafkaEnvironment.tearDown()
        }

        withTestApplicationForApi(TestApplicationEngine(), testDatabase) {
            it("Catch exception and don't rethrow it") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${generateJWT("1234")}")
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(primaryJob), enhetNr)
                    val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Created
                }

                val messages: MutableList<StatusEndring> = mutableListOf()
                testConsumer.poll(Duration.ofMillis(5000)).forEach {
                    val hendelse: StatusEndring =
                        objectMapper.readValue(it.value())
                    messages.add(hendelse)
                }
                messages.size shouldBeEqualTo 1
                val latestFlaggperson84Hendelse = messages.last()
                latestFlaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
                Thread.sleep(2000)
                verify { any<DatabaseInterface>().addStatus(any(), any(), any(), any()) }


            }
        }
    }
}) */
