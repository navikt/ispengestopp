package no.nav.syfo.testutils.mock

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.tilgangskontroll.TilgangDTO
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer.Companion.TILGANGSKONTROLL_PERSON_PATH
import no.nav.syfo.testutils.UserConstants.SYKMELDT_FNR
import no.nav.syfo.testutils.UserConstants.SYKMELDT_FNR_IKKE_TILGANG
import no.nav.syfo.testutils.getRandomPort
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

class VeilederTilgangskontrollMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    private val tilgangFalse = TilgangDTO(
        harTilgang = false,
    )
    private val tilgangTrue = TilgangDTO(
        harTilgang = true,
    )

    val name = "veiledertilgangskontroll"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get(TILGANGSKONTROLL_PERSON_PATH) {
                if (call.request.headers[NAV_PERSONIDENT_HEADER] == SYKMELDT_FNR.value) {
                    call.respond(tilgangTrue)
                }
                if (call.request.headers[NAV_PERSONIDENT_HEADER] == SYKMELDT_FNR_IKKE_TILGANG.value) {
                    call.respond(HttpStatusCode.Forbidden, tilgangFalse)
                }
            }
        }
    }
}
