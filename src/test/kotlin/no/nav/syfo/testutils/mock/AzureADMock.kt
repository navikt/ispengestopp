package no.nav.syfo.testutils.mock

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.installContentNegotiation
import no.nav.syfo.client.azuread.AzureAdTokenResponse
import no.nav.syfo.client.wellknown.WellKnown
import no.nav.syfo.testutils.getRandomPort
import java.nio.file.Paths

fun wellKnownInternADMock(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString()
    )
}

class AzureAdV2Mock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val aadV2TokenResponse = AzureAdTokenResponse(
        access_token = "token",
        expires_in = 3600,
        token_type = "type"
    )

    val name = "azureadv2"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post {
                call.respond(aadV2TokenResponse)
            }
        }
    }
}
