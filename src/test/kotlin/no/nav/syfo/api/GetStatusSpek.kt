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

class GetStatusSpek : Spek({

    val sykmeldtFnr = SykmeldtFnr("123456")
    val sykmeldtFnrFiller = SykmeldtFnr("654321")
    val sykmeldtFnrIkkeTilgang = SykmeldtFnr("666")
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val secondaryJob = VirksomhetNr("999")
    val enhetNr = EnhetNr("9999")
    val lastCreated = LocalDateTime.of(2020, Month.AUGUST, 7, 10, 10)
    val firstCreated = LocalDateTime.of(2020, Month.JULY, 8, 9, 9)
    val database by lazy { TestDB() }


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


        val mockServerPort = 9090
        val mockHttpServerUrl = "http://localhost:$mockServerPort"

        val mockServer =
            mockSyfotilgangskontrollServer(mockServerPort, sykmeldtFnr).start(wait = false)

        afterGroup { mockServer.stop(1L, 10L) }

        val uri = Paths.get(env.jwksUri).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()

        testApp.application.setupAuth(env, jwkProvider)

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

    describe("Get person flag status") {
        afterGroup {
            database.stop()
        }

        withTestApplicationForApi(TestApplicationEngine(), database) {
            it("reject request without bearer token") {
                with(handleRequest(HttpMethod.Get, "/api/v1/person/status") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("fnr", sykmeldtFnr.value)
                }) {
                    response.status() shouldBe io.ktor.http.HttpStatusCode.Unauthorized
                }
            }
            it("reject request to forbidden user") {
                with(handleRequest(HttpMethod.Get, "/api/v1/person/status") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("Authorization", "Bearer ${generateJWT("1234")}")
                    addHeader("fnr", sykmeldtFnrIkkeTilgang.value)
                }) {
                    response.status() shouldBe HttpStatusCode.Forbidden
                }
            }
            it("return correct content") {
                val statusList = listOf(
                    DBStatusChangeTest("1", sykmeldtFnr, veilederIdent, Status.STOPP_AUTOMATIKK, primaryJob, enhetNr, lastCreated),
                    DBStatusChangeTest("2", sykmeldtFnr, veilederIdent, Status.STOPP_AUTOMATIKK, primaryJob, enhetNr, firstCreated),
                    DBStatusChangeTest("3", sykmeldtFnr, veilederIdent, Status.STOPP_AUTOMATIKK, secondaryJob, enhetNr, lastCreated),
                    DBStatusChangeTest("4", sykmeldtFnrFiller, veilederIdent, Status.STOPP_AUTOMATIKK, primaryJob, enhetNr, lastCreated))
                statusList.forEach {database.connection.addStatus(it)}

                with(handleRequest(HttpMethod.Get, "/api/v1/person/status") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader("Authorization", "Bearer ${generateJWT("1234")}")
                    addHeader("fnr", sykmeldtFnr.value)
                }) {
                    response.status() shouldBe HttpStatusCode.OK
                    val flags = Gson().fromJson(response.content!!, Array<StatusEndring>::class.java).toList()
                    flags.size shouldBeEqualTo 2
                    flags[0].sykmeldtFnr.value shouldBeEqualTo sykmeldtFnr.value
                    flags[0].opprettet shouldBeEqualTo lastCreated
                }
            }
        }
    }
})
