package no.nav.syfo.testutils.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.client.wellknown.WellKnown
import java.nio.file.Paths

fun wellKnownInternADMock(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString()
    )
}

fun MockRequestHandleScope.azureAdMockResponse(): HttpResponseData = respond(
    content = """{"access_token":"token","expires_in":3600,"token_type":"Bearer"}""",
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)
