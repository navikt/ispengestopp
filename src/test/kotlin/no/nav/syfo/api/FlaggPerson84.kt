package no.nav.syfo.api

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import no.nav.syfo.Environment
import org.amshove.kluent.shouldBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class FlaggPerson84 : Spek({

    fun withTestApplicationForApi(testApp: TestApplicationEngine, block: TestApplicationEngine.() -> Unit) {
        testApp.start()
        val environment = Environment(
            "ispengestopp",
            8080,
            "",
            "",
            "",
            "",
            false
        )

        testApp.application.routing {
            route("/api") {
                registerFlaggPerson84()
            }
        }

        return testApp.block()
    }

    describe("Flag a person to be removed from automatic processing") {
        withTestApplicationForApi(TestApplicationEngine()) {
            it("return 200") {

                with(handleRequest(HttpMethod.Post, "/api/v1/person/flagg") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("{\"fnr\": \"123456\"}")
                }) {
                    response.status() shouldBe HttpStatusCode.OK
                }
            }
        }
    }
})
