package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.google.gson.Gson
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.setupAuth
import no.nav.syfo.kafka.GsonKafkaSerializer
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.`should be greater or equal to`
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

@KtorExperimentalAPI
class PostStatusSpek : Spek({

    val sykmeldtFnr = SykmeldtFnr("123456")
    val sykmeldtFnrIkkeTilgang = SykmeldtFnr("666")
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val secondaryJob = VirksomhetNr("999")
    val enhetNr = EnhetNr("9999")
    val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        topicNames = listOf("aapen-isyfo-person-flagget84")
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
        "aapen-isyfo-person-flagget84"
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
    testConsumer.subscribe(listOf(env.flaggPerson84Topic))

    val prodConsumerProperties = baseConfig
        .toConsumerConfig("prodConsumer", valueDeserializer = StringDeserializer::class)

    val producerProperties = baseConfig.toProducerConfig("spek.integration-producer", GsonKafkaSerializer::class)
    val personFlagget84Producer = KafkaProducer<String, KFlaggperson84Hendelse>(producerProperties)

    //TODO gjøre database delen av testen om til å gi mer test coverage av prodkoden
    fun withTestApplicationForApi(
        testApp: TestApplicationEngine,
        database: TestDB,
        block: TestApplicationEngine.() -> Unit
    ) {
        testApp.start()
        testApp.application.install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
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
            database.connection.dropData()
        }
        prodConsumer.subscribe(listOf(env.flaggPerson84Topic))
        applicationState.ready = true

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
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(primaryJob), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
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
                        StoppAutomatikk(sykmeldtFnrIkkeTilgang, listOf(primaryJob), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Forbidden
                }
            }
            it("return correct status code") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        "Authorization",
                        "Bearer ${generateJWT("1234")}"
                    )
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(secondaryJob), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Created
                }
            }
            it("persist status change to kafka and database") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${generateJWT("1234")}")
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(primaryJob), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Created
                }

                val messages: ArrayList<KFlaggperson84Hendelse> = arrayListOf()
                testConsumer.poll(Duration.ofMillis(5000)).forEach {
                    val hendelse: KFlaggperson84Hendelse =
                        Gson().fromJson(it.value(), KFlaggperson84Hendelse::class.java)
                    messages.add(hendelse)
                }

                messages.size `should be greater or equal to` 1

                val latestFlaggperson84Hendelse = messages.last()
                latestFlaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
                latestFlaggperson84Hendelse.veilederIdent shouldBeEqualTo veilederIdent
                latestFlaggperson84Hendelse.virksomhetNr shouldBeEqualTo primaryJob
                latestFlaggperson84Hendelse.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
                latestFlaggperson84Hendelse.opprettet.dayOfMonth shouldBeEqualTo Instant.now()
                    .atZone(ZoneId.systemDefault()).dayOfMonth
                latestFlaggperson84Hendelse.enhetNr shouldBeEqualTo enhetNr

                Thread.sleep(1500) // Make sure the listener coroutine is done reading from the kafka topic

                val statusendringListe = database.connection.hentStatusEndringListe(sykmeldtFnr, primaryJob)
                statusendringListe.size shouldBeEqualTo 1

                val statusEndring = statusendringListe[0]
                statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
                statusEndring.veilederIdent shouldBeEqualTo veilederIdent
                statusEndring.virksomhetNr shouldBeEqualTo primaryJob
                statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
                LocalDate.parse(statusEndring.opprettet, DateTimeFormatter.ISO_LOCAL_DATE_TIME) shouldBeEqualTo
                        Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
                statusEndring.enhetNr shouldBeEqualTo enhetNr
            }
        }
    }
})
