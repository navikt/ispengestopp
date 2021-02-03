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
import io.ktor.http.HttpHeaders.Authorization
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
import no.nav.syfo.util.bearerHeader
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

    val sykmeldtFnr = UserConstants.SYKMELDT_FNR
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

    val env = testEnvironment(embeddedKafkaEnvironment.brokersURL)
    val credentials = testVaultSecrets()

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

        val veilederTilgangskontrollMock = VeilederTilgangskontrollMock()

        testApp.application.install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }

        val uri = Paths.get(env.jwksUri).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()

        testApp.application.setupAuth(env, jwkProvider)

        val tilgangskontrollConsumer = TilgangskontrollConsumer(
            url = "${veilederTilgangskontrollMock.url}/syfo-tilgangskontroll/api/tilgang/bruker"
        )

        testApp.application.routing {
            authenticate {
                registerFlaggPerson84(
                    database,
                    env,
                    personFlagget84Producer,
                    tilgangskontrollConsumer
                )
            }
        }

        beforeGroup {
            veilederTilgangskontrollMock.server.start()
        }

        afterGroup {
            veilederTilgangskontrollMock.server.stop(1L, 10L)
        }

        afterEachTest {
            database.connection.dropData()
        }

        return testApp.block()
    }

    describe("Get person flag status") {
        afterGroup {
            database.stop()
        }

        withTestApplicationForApi(TestApplicationEngine(), database) {
            val endpointPath = "$apiBasePath$apiPersonStatusPath"
            val validToken = generateJWT(
                env.loginserviceClientId,
            )
            it("reject request without bearer token") {
                with(
                    handleRequest(HttpMethod.Get, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader("fnr", sykmeldtFnr.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }
            it("reject request to forbidden user") {
                with(
                    handleRequest(HttpMethod.Get, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(Authorization, bearerHeader(validToken))
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
                    handleRequest(HttpMethod.Get, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(Authorization, bearerHeader(validToken))
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
