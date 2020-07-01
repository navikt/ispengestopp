package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.syfo.COUNT_ENDRE_PERSON_STATUS_FORBIDDEN
import no.nav.syfo.COUNT_ENDRE_PERSON_STATUS_SUCCESS
import no.nav.syfo.StoppAutomatikk
import no.nav.syfo.addFlagg
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.Flaggperson84Kt")

fun Route.registerFlaggPerson84(
    database: DatabaseInterface,
    tilgangskontroll: TilgangskontrollConsumer
) {
    route("/api/v1") {
        post("/person/flagg") {
            val stoppAutomatikk: StoppAutomatikk = call.receive()
            log.info("Received call to /api/v1/person/flagg")

            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")

            if (token == null) {
                call.respond(HttpStatusCode.Forbidden)
            }

            val harTilgang = tilgangskontroll.harTilgangTilBruker(stoppAutomatikk.sykmeldtFnr, token!!)

            if (harTilgang) {
                stoppAutomatikk.virksomhetNr.forEach {
                    database.addFlagg(
                        stoppAutomatikk.sykmeldtFnr,
                        stoppAutomatikk.veilederIdent,
                        stoppAutomatikk.enhetNr,
                        it
                    )
                }
                COUNT_ENDRE_PERSON_STATUS_SUCCESS.inc()
                call.respond(HttpStatusCode.Created)
            } else {
                COUNT_ENDRE_PERSON_STATUS_FORBIDDEN.inc()
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}
