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
import org.slf4j.LoggerFactory

class TilgangskontrollConsumer(
    private val url: String = "http://syfo-tilgangskontroll/syfo-tilgangskontroll/api/tilgang/bruker"
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
            val response: HttpResponse = httpClient.get("$url?fnr=${fnr.value}") {
                header(HttpHeaders.Authorization, "Bearer $token")
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
