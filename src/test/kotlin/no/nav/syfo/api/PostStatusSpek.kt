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
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.*
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
import java.time.*
import java.util.*

class PostStatusSpek : Spek({

    val sykmeldtFnr = SykmeldtFnr("123456")
    val sykmeldtFnrIkkeTilgang = SykmeldtFnr("666")
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
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

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val baseConfig = loadBaseConfig(env, credentials).overrideForTest()
    val consumerProperties = baseConfig
        .toConsumerConfig("spek.integration-consumer", valueDeserializer = StringDeserializer::class)
    val consumer = KafkaConsumer<String, String>(consumerProperties)
    consumer.subscribe(listOf(env.flaggPerson84Topic))

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

        beforeEachTest {
        }

        afterEachTest {
            database.connection.dropData()
        }

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
            it("unauthorized response") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(primaryJob), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }
            it("forbidden response") {
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
            it("reponse 200 OK") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        "Authorization",
                        "Bearer ${generateJWT("1234")}"
                    )
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(primaryJob), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Created
                }
            }
            it("store in database") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer ${generateJWT("1234")}")
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(primaryJob), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Created
                }

                val statusendringListe = database.connection.hentStatusEndringListe(sykmeldtFnr, primaryJob)
                statusendringListe.size shouldBeEqualTo 1

                val statusEndring = statusendringListe[0]
                statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
                statusEndring.veilederIdent shouldBeEqualTo veilederIdent
                statusEndring.virksomhetNr shouldBeEqualTo primaryJob
                statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
                statusEndring.opprettet.dayOfMonth shouldBeEqualTo Instant.now()
                    .atZone(ZoneId.systemDefault()).dayOfMonth
                statusEndring.enhetNr shouldBeEqualTo enhetNr
                val messages: ArrayList<KFlaggperson84Hendelse> = arrayListOf()
                consumer.poll(Duration.ofMillis(5000)).forEach {
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
            }
        }
    }
})
