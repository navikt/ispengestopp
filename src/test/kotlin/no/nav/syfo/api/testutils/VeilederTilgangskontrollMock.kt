package no.nav.syfo.api.testutils

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.api.testutils.UserConstants.SYKMELDT_FNR
import no.nav.syfo.api.testutils.UserConstants.SYKMELDT_FNR_IKKE_TILGANG
import no.nav.syfo.application.installContentNegotiation
import no.nav.syfo.client.tilgangskontroll.TilgangDTO

class VeilederTilgangskontrollMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    private val tilgangFalse = TilgangDTO(
        false,
        ""
    )
    private val tilgangTrue = TilgangDTO(
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
        tilgangFalse: TilgangDTO,
        tilgangTrue: TilgangDTO,
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
                get("/syfo-tilgangskontroll/api/tilgang/navident/bruker/${SYKMELDT_FNR.value}") {
                    call.respond(tilgangTrue)
                }
                get("/syfo-tilgangskontroll/api/tilgang/navident/bruker/${SYKMELDT_FNR_IKKE_TILGANG.value}") {
                    call.respond(HttpStatusCode.Forbidden, tilgangFalse)
                }
            }
        }
    }
}
