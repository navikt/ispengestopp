package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
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
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.*
import no.nav.syfo.application.setupAuth
import no.nav.syfo.kafka.kafkaPersonFlaggetConsumerProperties
import no.nav.syfo.kafka.kafkaPersonFlaggetProducerProperties
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths
import java.util.*

class GetStatusSpek : Spek({

    val sykmeldtFnr = SykmeldtFnr("123456")
    val sykmeldtFnrFiller = SykmeldtFnr("654321")
    val sykmeldtFnrIkkeTilgang = SykmeldtFnr("666")
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val secondaryJob = VirksomhetNr("999")
    val enhetNr = EnhetNr("9999")
    val database by lazy { TestDB() }

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
        0
    )
    val credentials = VaultSecrets(
        "",
        ""
    )

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val consumerProperties = kafkaPersonFlaggetConsumerProperties(env, credentials)
        .overrideForTest()
    val consumer = KafkaConsumer<String, String>(consumerProperties)
    consumer.subscribe(listOf(env.stoppAutomatikkTopic))

    val producerProperties = kafkaPersonFlaggetProducerProperties(env, credentials)
    val personFlagget84Producer = KafkaProducer<String, StatusEndring>(producerProperties)

    // TODO: gjøre database delen av testen om til å gi mer test coverage av prodkoden
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
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
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
                with(
                    handleRequest(HttpMethod.Get, "/api/v1/person/status") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader("fnr", sykmeldtFnr.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }
            it("reject request to forbidden user") {
                with(
                    handleRequest(HttpMethod.Get, "/api/v1/person/status") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader("Authorization", "Bearer ${generateJWT("1234")}")
                        addHeader("fnr", sykmeldtFnrIkkeTilgang.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Forbidden
                }
            }
            it("return correct content") {
                val arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
                    Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
                )

                val statusList = listOf(
                    DBStatusChangeTest(
                        "1",
                        sykmeldtFnr,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        primaryJob,
                        enhetNr
                    ),
                    DBStatusChangeTest(
                        "2",
                        sykmeldtFnr,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        primaryJob,
                        enhetNr
                    ),
                    DBStatusChangeTest(
                        "3",
                        sykmeldtFnr,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        secondaryJob,
                        enhetNr
                    ),
                    DBStatusChangeTest(
                        "4",
                        sykmeldtFnrFiller,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        primaryJob,
                        enhetNr
                    )
                )
                statusList.forEach {
                    database.addStatus(
                        it.uuid,
                        it.sykmeldtFnr,
                        it.veilederIdent,
                        it.enhetNr,
                        it.arsakList,
                        it.virksomhetNr
                    )
                }

                with(
                    handleRequest(HttpMethod.Get, "/api/v1/person/status") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader("Authorization", "Bearer ${generateJWT("1234")}")
                        addHeader("fnr", sykmeldtFnr.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.OK

                    val flags: List<StatusEndring> = objectMapper.readValue(response.content!!)

                    flags.size shouldBeEqualTo 3
                    flags.first().sykmeldtFnr.value shouldBeEqualTo sykmeldtFnr.value
                    flags.first().arsakList shouldBeEqualTo arsakList
                    flags.first().opprettet.toEpochSecond()
                        .shouldBeGreaterOrEqualTo(flags.last().opprettet.toEpochSecond())
                }
            }
        }
    }
})
