package no.nav.syfo.testutils.mock

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.tilgangskontroll.TilgangDTO
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer.Companion.TILGANGSKONTROLL_PERSON_PATH
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG
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
                if (call.request.headers[NAV_PERSONIDENT_HEADER] == SYKMELDT_PERSONIDENT.value) {
                    call.respond(tilgangTrue)
                }
                if (call.request.headers[NAV_PERSONIDENT_HEADER] == SYKMELDT_PERSONIDENT_IKKE_TILGANG.value) {
                    call.respond(HttpStatusCode.Forbidden, tilgangFalse)
                }
            }
        }
    }
}
