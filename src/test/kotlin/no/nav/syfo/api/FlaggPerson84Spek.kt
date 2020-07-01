package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.api.testutils.dropData
import no.nav.syfo.api.testutils.generateJWT
import no.nav.syfo.api.testutils.hentStatusEndringListe
import no.nav.syfo.application.setupAuth
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId

class FlaggPerson84Spek : Spek({

    val sykmeldtFnr = SykmeldtFnr("123456")
    val sykmeldtFnrIkkeTilgang = SykmeldtFnr("666")
    val veilederIdent = VeilederIdent("Z999999")
    val virksomhetNr = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")

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

        val mockHttpServerUrl = "http://localhost:9090"
        val mockServer = embeddedServer(Netty, 9090) {
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }
            routing {
                get("/syfo-tilgangskontroll/api/tilgang/bruker") {
                    if (call.request.queryParameters["fnr"]!! == sykmeldtFnr.value) {
                        call.respond(Tilgang(true))
                    } else {
                        call.respond(Tilgang(false, "Vil ikke"))
                    }
                }
            }
        }.start(wait = false)

        val env = Environment(
            "ispengestopp",
            8080,
            "",
            "",
            "",
            "",
            "https://sts.issuer.net/myid",
            "src/test/resources/jwkset.json",
            false,
            "1234"
        )

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
                    TilgangskontrollConsumer("$mockHttpServerUrl/syfo-tilgangskontroll/api/tilgang/bruker")
                )
            }
        }

        afterGroup { mockServer.stop(1L, 10L) }

        return testApp.block()
    }

    describe("Flag a person to be removed from automatic processing") {
        val database by lazy { TestDB() }
        afterGroup {
            database.stop()
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
            }
        }
    }
})

