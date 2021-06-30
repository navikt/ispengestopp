package no.nav.syfo.api.testutils

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.Tilgang
import no.nav.syfo.api.testutils.UserConstants.SYKMELDT_FNR
import no.nav.syfo.application.installContentNegotiation

class VeilederTilgangskontrollMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    private val tilgangFalse = Tilgang(
        false,
        ""
    )
    private val tilgangTrue = Tilgang(
        true,
        ""
    )

    val name = "veiledertilgangskontroll"
    val server = mockTilgangServer(
        port,
        tilgangFalse,
        tilgangTrue
    )

    private fun mockTilgangServer(
        port: Int,
        tilgangFalse: Tilgang,
        tilgangTrue: Tilgang,
    ): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port
        ) {
            installContentNegotiation()
            routing {
                get("/syfo-tilgangskontroll/api/tilgang/bruker") {
                    when {
                        call.parameters["fnr"] == SYKMELDT_FNR.value -> {
                            call.respond(tilgangTrue)
                        }
                        else -> {
                            call.respond(HttpStatusCode.Forbidden, tilgangFalse)
                        }
                    }
                }
            }
        }
    }
}
