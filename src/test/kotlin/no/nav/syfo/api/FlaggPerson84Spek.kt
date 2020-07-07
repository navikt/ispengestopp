package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.google.gson.Gson
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.client.features.json.GsonSerializer
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
import kotlinx.serialization.serializer
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.*
import no.nav.syfo.application.setupAuth
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.*

class FlaggPerson84Spek : Spek({

    val sykmeldtFnr = SykmeldtFnr("123456")
    val sykmeldtFnrIkkeTilgang = SykmeldtFnr("666")
    val veilederIdent = VeilederIdent("Z999999")
    val virksomhetNr = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")


    val embeddedEnvironment = KafkaEnvironment( // TODO Kan vi endre navn på denne? Jeg synes det er vanskeligere å skjønne hva embeddedEnvironment er lenger ned i koden.
        autoStart = false,
        topicNames = listOf("aapen-isyfo-person-flagget84")
    )

    val env = Environment(
        "ispengestopp",
        8080,
        embeddedEnvironment.brokersURL,
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

        beforeEachTest {
            embeddedEnvironment.start()
        }

        afterEachTest {
            database.connection.dropData()
            embeddedEnvironment.tearDown() // TODO Jeg tror dette er en teit måte å gjøre det på, men hvis vi har flere tester som kommer til kafka-delen, vil vi får flere elemtener på køen, og dette kan tømme den. Men nå fikk jeg tom liste i assertion :(
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
            embeddedEnvironment.start()
        }
        afterGroup {
            database.stop()
            embeddedEnvironment.tearDown()
        }

        withTestApplicationForApi(TestApplicationEngine(), database) {

            it("response should be unauthorized") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(virksomhetNr), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }

            it("Forbidden") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        "Authorization",
                        "Bearer ${generateJWT("1234")}"
                    )
                    val stoppAutomatikk =
                        StoppAutomatikk(sykmeldtFnrIkkeTilgang, listOf(virksomhetNr), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Forbidden
                }
            }

            it("200 OK") {
                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(
                        "Authorization",
                        "Bearer ${generateJWT("1234")}"
                    )
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(virksomhetNr), veilederIdent, enhetNr)
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
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(virksomhetNr), veilederIdent, enhetNr)
                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.Created
                }

                val statusendringListe = database.connection.hentStatusEndringListe(sykmeldtFnr, virksomhetNr)
                statusendringListe.size shouldBeEqualTo 1

                val statusEndring = statusendringListe[0]
                statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
                statusEndring.veilederIdent shouldBeEqualTo veilederIdent
                statusEndring.virksomhetNr shouldBeEqualTo virksomhetNr
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
                messages.size shouldBeEqualTo 1
                val flaggperson84Hendelse = messages.first()
                flaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
            }
        }
    }
})

// TODO Jeg vet ikke helt hva vi skal gjøre med denne! :( Jeg vet ikke hvorfor "toProducerConfig" trenger en så rar serializer-klasse.
// TODO Den ekte Producer-genereringen bruker ikke denn nå, men "StringSerializer::class", og det vil nok ikke fungere, siden vi sender inn et objekt.
class GsonKafkaSerializer(): Serializer<Any> {
    private val gsonMapper = Gson()
    override fun serialize(topic: String?, data: Any?): ByteArray = gsonMapper.toJson(data).toByteArray()
    override fun close() {}
}
