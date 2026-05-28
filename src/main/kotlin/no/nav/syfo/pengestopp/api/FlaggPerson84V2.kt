package no.nav.syfo.pengestopp.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.common.tilgangskontroll.client.TilgangskontrollClient
import no.nav.syfo.common.tilgangskontroll.ktor.checkVeilederTilgangToPerson
import no.nav.syfo.common.util.ktor.getPersonIdent
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*

const val apiV2BasePath = "/api/v2"
const val apiV2PersonStatusPath = "/person/status"

fun Route.registerFlaggPerson84V2(
    pengestoppService: PengestoppService,
    tilgangskontrollClient: TilgangskontrollClient,
) {
    route(apiV2BasePath) {
        get(apiV2PersonStatusPath) {
            val personIdentStr = call.getPersonIdent()
                ?: throw IllegalArgumentException("No Personident for sykmeldt supplied")
            checkVeilederTilgangToPerson(
                action = "get person status",
                personIdent = personIdentStr,
                tilgangskontrollClient = tilgangskontrollClient,
            ) {
                val personIdent = PersonIdent(personIdentStr)
                val flags: List<StatusEndring> = pengestoppService.getManuelleStatusendringer(personIdent)
                when {
                    flags.isNotEmpty() -> call.respond(flags)
                    else -> call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
