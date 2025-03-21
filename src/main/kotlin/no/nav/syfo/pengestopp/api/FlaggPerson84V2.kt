package no.nav.syfo.pengestopp.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.callIdArgument
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.Flaggperson84V2Kt")

const val apiV2BasePath = "/api/v2"
const val apiV2PersonStatusPath = "/person/status"

fun Route.registerFlaggPerson84V2(
    pengestoppService: PengestoppService,
    tilgangskontrollClient: TilgangskontrollClient,
) {
    route(apiV2BasePath) {
        get(apiV2PersonStatusPath) {
            val callId = getCallId()

            try {
                val sykmeldtPersonident = call.request.headers[NAV_PERSONIDENT_HEADER]
                    ?: throw IllegalArgumentException("No Personident for sykmeldt supplied")

                val token = getBearerHeader() ?: throw IllegalArgumentException("No Authorization header supplied")

                val personIdent = PersonIdent(sykmeldtPersonident)
                val hasAccess = tilgangskontrollClient.harTilgangTilBrukerMedOBO(personIdent, token)
                if (hasAccess) {
                    val flags: List<StatusEndring> = pengestoppService.getStatusendringer(personIdent)
                    when {
                        flags.isNotEmpty() -> call.respond(flags)
                        else -> call.respond(HttpStatusCode.NoContent)
                    }
                } else {
                    COUNT_GET_PERSON_STATUS_FORBIDDEN.increment()
                    call.respond(HttpStatusCode.Forbidden)
                }
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Request to get for PersonIdent"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callIdArgument(callId))
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
