package no.nav.syfo.testutils.mock

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.pdl.*
import no.nav.syfo.testutils.UserConstants
import no.nav.syfo.testutils.getRandomPort

fun generatePdlIdenter(
    personident: String,
) = PdlIdenterResponse(
    data = PdlHentIdenter(
        hentIdenter = PdlIdenter(
            identer = listOf(
                PdlIdent(
                    ident = personident,
                    historisk = false,
                    gruppe = IdentType.FOLKEREGISTERIDENT,
                ),
                PdlIdent(
                    ident = "9${personident.drop(1)}",
                    historisk = true,
                    gruppe = IdentType.FOLKEREGISTERIDENT,
                ),
            ),
        ),
    ),
    errors = null,
)

class PdlMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val name = "pdl"

    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post {
                val pdlRequest = call.receive<PdlHentIdenterRequest>()
                if (pdlRequest.variables.ident == UserConstants.ARBEIDSTAKER_PERSONIDENT_3.value) {
                    call.respond(generatePdlIdenter("enAnnenIdent"))
                } else {
                    call.respond(generatePdlIdenter(pdlRequest.variables.ident))
                }
            }
        }
    }
}
