package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.syfo.COUNT_ENDRE_PERSON_STATUS_SUCCESS
import no.nav.syfo.StoppAutomatikk
import no.nav.syfo.addFlagg
import no.nav.syfo.database.DatabaseInterface
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.Flaggperson84Kt")

fun Route.registerFlaggPerson84(
    database: DatabaseInterface
) {
    route("/api/v1") {
        post("/person/flagg") {
            val stoppAutomatikk: StoppAutomatikk = call.receive()
            log.info("Received call to /api/v1/person/flagg")

            stoppAutomatikk.virksomhetNr.forEach {
                database.addFlagg(stoppAutomatikk.sykmeldtFnr, stoppAutomatikk.veilederIdent, stoppAutomatikk.enhetNr, it)
            }

            COUNT_ENDRE_PERSON_STATUS_SUCCESS.inc()
            call.respondText("{}", status = HttpStatusCode.OK)
        }
    }
}
