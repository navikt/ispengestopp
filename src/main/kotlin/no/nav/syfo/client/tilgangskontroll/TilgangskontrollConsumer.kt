package no.nav.syfo.client.tilgangskontroll

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.client.azuread.v2.AzureAdV2Client
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class TilgangskontrollConsumer(
    private val azureAdV2Client: AzureAdV2Client,
    private val syfotilgangskontrollClientId: String,
    tilgangskontrollBaseUrl: String
) {
    private val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    private val tilgangskontrollPersonUrl: String

    init {
        tilgangskontrollPersonUrl = "$tilgangskontrollBaseUrl$TILGANGSKONTROLL_PERSON_PATH"
    }

    suspend fun harTilgangTilBrukerMedOBO(
        fnr: SykmeldtFnr,
        token: String,
    ): Boolean {
        val oboToken = azureAdV2Client.getOnBehalfOfToken(
            scopeClientId = syfotilgangskontrollClientId,
            token = token
        )?.accessToken ?: throw RuntimeException("Failed to request access to Person: Failed to get OBO token")

        try {
            val response: HttpResponse = httpClient.get(tilgangskontrollPersonUrl) {
                header(NAV_PERSONIDENT_HEADER, fnr.value)
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                accept(ContentType.Application.Json)
            }
            COUNT_TILGANGSKONTROLL_OK.inc()
            return response.receive<TilgangDTO>().harTilgang
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

    companion object {
        private val log = LoggerFactory.getLogger(TilgangskontrollConsumer::class.java)
        const val TILGANGSKONTROLL_PERSON_PATH = "/syfo-tilgangskontroll/api/tilgang/navident/person"
    }
}
