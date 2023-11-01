package no.nav.syfo.testutils.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.tilgangskontroll.TilgangDTO
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.tilgangskontrollMockResponse(request: HttpRequestData): HttpResponseData {
    return when (request.headers[NAV_PERSONIDENT_HEADER]) {
        SYKMELDT_PERSONIDENT_IKKE_TILGANG.value -> respond(TilgangDTO(erGodkjent = false))
        else -> respond(TilgangDTO(erGodkjent = true))
    }
}
