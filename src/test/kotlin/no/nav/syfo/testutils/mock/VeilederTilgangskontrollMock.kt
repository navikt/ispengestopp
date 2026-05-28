package no.nav.syfo.testutils.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG

private data class TilgangDTO(val erGodkjent: Boolean, val fullTilgang: Boolean = erGodkjent)

fun MockRequestHandleScope.tilgangskontrollMockResponse(request: HttpRequestData): HttpResponseData {
    return when (request.headers[NAV_PERSONIDENT_HEADER]) {
        SYKMELDT_PERSONIDENT_IKKE_TILGANG.value -> respond(TilgangDTO(erGodkjent = false))
        else -> respond(TilgangDTO(erGodkjent = true))
    }
}
