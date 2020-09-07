package no.nav.syfo.api

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
import kotlinx.coroutines.InternalCoroutinesApi
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.setupAuth
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
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@InternalCoroutinesApi
@KtorExperimentalAPI
class PostStatusSpek : Spek({
    val sykmeldtFnr = SykmeldtFnr("123456")
    val sykmeldtFnrIkkeTilgang = SykmeldtFnr("666")
    val veilederIdent = VeilederIdent("Z999999")
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
        "apen-isyfo-stoppautomatikk",
        0L
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

    fun withTestApplicationForApi(
        testApp: TestApplicationEngine,
        testDB: TestDB,
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

        val mockServerPort = 9091
        val mockHttpServerUrl = "http://localhost:$mockServerPort"

        val mockServer =
            mockSyfotilgangskontrollServer(mockServerPort, sykmeldtFnr).start(wait = false)

        afterGroup { mockServer.stop(1L, 10L) }

        val uri = Paths.get(env.jwksUri).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()

        testApp.application.setupAuth(env, jwkProvider)

        val prodConsumer = KafkaConsumer<String, String>(prodConsumerProperties)
        afterEachTest {
            testDB.connection.dropData()
        }
        prodConsumer.subscribe(listOf(env.stoppAutomatikkTopic))
        applicationState.ready.set(true)

        launchListeners(
            applicationState,
            testDB,
            prodConsumer,
            env
        )

        testApp.application.routing {
            authenticate {
                registerFlaggPerson84(
                    testDB,
                    env,
                    personFlagget84Producer,
                    TilgangskontrollConsumer("$mockHttpServerUrl/syfo-tilgangskontroll/api/tilgang/bruker")
                )
            }
        }

        return testApp.block()
    }

    describe("Flag a person to be removed from automatic processing") {
        val database by lazy { TestDB() }
        beforeGroup {
            embeddedKafkaEnvironment.start()
        }
        afterGroup {
            database.stop()
            embeddedKafkaEnvironment.tearDown()
        }

        withTestApplicationForApi(TestApplicationEngine(), database) {
            it("reject post request without token") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(primaryJob), enhetNr)
                    val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }
            it("reject post request to forbidden user") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        "Authorization",
                        "Bearer ${generateJWT("1234")}"
                    )
                    val stoppAutomatikk =
                        StoppAutomatikk(sykmeldtFnrIkkeTilgang, listOf(primaryJob), enhetNr)
                    val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Forbidden
                }
            }
            it("persist status change to kafka and database") {
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
                latestFlaggperson84Hendelse.veilederIdent shouldBeEqualTo veilederIdent
                latestFlaggperson84Hendelse.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
                latestFlaggperson84Hendelse.opprettet.dayOfMonth shouldBeEqualTo Instant.now()
                    .atZone(ZoneOffset.UTC).dayOfMonth
                latestFlaggperson84Hendelse.enhetNr shouldBeEqualTo enhetNr
                latestFlaggperson84Hendelse.virksomhetNr shouldBeEqualTo primaryJob

            }
        }
    }
})

