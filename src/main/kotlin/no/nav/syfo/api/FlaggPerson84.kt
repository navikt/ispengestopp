package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.syfo.log

fun Route.registerFlaggPerson84() {
    route("/v1") {
        post("/person/flagg") {
            val body = call.receiveText() // val obj: T = call.receive<T>()
            log.info("Recived call to /api/v1/person/")
            log.info("Body $body")

            call.respondText("{}", status = HttpStatusCode.OK)
        }
    }
}
