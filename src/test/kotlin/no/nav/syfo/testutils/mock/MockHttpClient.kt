package no.nav.syfo.testutils.mock

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import no.nav.syfo.application.Environment
import no.nav.syfo.client.commonConfig

fun mockHttpClient(environment: Environment) = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler { request ->
            val requestUrl = request.url.encodedPath
            when {
                requestUrl == "/${environment.azureTokenEndpoint}" -> azureAdMockResponse()
                requestUrl.startsWith("/${environment.tilgangskontrollUrl}") -> tilgangskontrollMockResponse(
                    request
                )
                requestUrl.startsWith("/${environment.pdlUrl}") -> pdlMockResponse(request)

                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
    }
}
