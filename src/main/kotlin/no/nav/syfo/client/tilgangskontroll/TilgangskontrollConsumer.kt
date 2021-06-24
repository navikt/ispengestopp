package no.nav.syfo.client.tilgangskontroll

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class TilgangskontrollConsumer(
    private val tilgangskontrollBaseUrl: String
) {
    private val log = LoggerFactory.getLogger("no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer")

    @KtorExperimentalAPI
    private val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    suspend fun harTilgangTilBruker(fnr: SykmeldtFnr, token: String): Boolean {
        try {
            val response: HttpResponse = httpClient.get(getTilgangskontrollUrl(fnr)) {
                header(HttpHeaders.Authorization, bearerHeader(token))
                accept(ContentType.Application.Json)
            }
            COUNT_TILGANGSKONTROLL_OK.inc()
            return response.receive<Tilgang>().harTilgang
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                COUNT_TILGANGSKONTROLL_FORBIDDEN.inc()
                false
            } else {
                return handleUnexpectedReponseException(e.response)
            }
        } catch (e: ServerResponseException) {
            return handleUnexpectedReponseException(e.response)
        }
    }

    private fun getTilgangskontrollUrl(fnr: SykmeldtFnr): String {
        return "$tilgangskontrollBaseUrl/syfo-tilgangskontroll/api/tilgang/bruker?fnr=${fnr.value}"
    }

    private fun handleUnexpectedReponseException(response: HttpResponse): Boolean {
        val statusCode = response.status.value.toString()
        log.error(
            "Error while requesting access to person from syfo-tilgangskontroll with {}",
            StructuredArguments.keyValue("statusCode", statusCode)
        )
        COUNT_TILGANGSKONTROLL_FAIL.labels(statusCode).inc()
        return false
    }
}
