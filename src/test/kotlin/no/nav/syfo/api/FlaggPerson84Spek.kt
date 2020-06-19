package no.nav.syfo.api

import com.google.gson.Gson
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.api.testutils.dropData
import no.nav.syfo.api.testutils.hentStatusEndringListe
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


// TODO jeg endret dette klassenavnet + filnavnet til å ha postfix "Test", deretter kjørte jeg v2 aka FlaggPerson84Spek, og da gikk det gjennom :O :D Woooot!
class FlaggPerson84Spek : Spek({

    val sykmeldtFnr = SykmeldtFnr("123456")
    val veilederIdent = VeilederIdent("Z999999")
    val virksomhetNr = VirksomhetNr("123")

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
        val environment = Environment(
            "ispengestopp",
            8080,
            "",
            "",
            "",
            "",
            false
        )

        beforeEachTest {
        }

        afterEachTest {
            database.connection.dropData()
        }

        testApp.application.routing {
            registerFlaggPerson84(database)
        }

        return testApp.block()
    }

    describe("Flag a person to be removed from automatic processing") {

        val database by lazy { TestDB() }

        afterGroup {
            database.stop()
        }

        withTestApplicationForApi(TestApplicationEngine(), database) {
            it("return 200") {

                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(virksomhetNr), veilederIdent)

                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)

                }) {
                    response.status() shouldBe HttpStatusCode.OK
                }
            }

            it("store in database") {

                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, listOf(virksomhetNr), veilederIdent)

                    val stoppAutomatikkJson = Gson().toJson(stoppAutomatikk)
                    setBody(stoppAutomatikkJson)
                }) {
                    response.status() shouldBe HttpStatusCode.OK
                }

                //database.connection.testInsert(sykmeldtFnr, veilederIdent, virksomhetNr)

                val statusendringListe = database.connection.hentStatusEndringListe(sykmeldtFnr, virksomhetNr)
                statusendringListe.size shouldBeEqualTo  1

                val statusEndring = statusendringListe[0]

                statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
                statusEndring.veilederIdent shouldBeEqualTo veilederIdent
                statusEndring.virksomhetNr shouldBeEqualTo virksomhetNr
                statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK

                database.connection.dropData()

            }
        }
    }
})

