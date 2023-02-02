package no.nav.syfo.client.tilgangskontroll

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Metrics
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class TilgangskontrollConsumer(
    private val azureAdClient: AzureAdClient,
    private val syfotilgangskontrollClientId: String,
    tilgangskontrollBaseUrl: String
) {
    private val httpClient = httpClientDefault()

    private val tilgangskontrollPersonUrl: String

    init {
        tilgangskontrollPersonUrl = "$tilgangskontrollBaseUrl$TILGANGSKONTROLL_PERSON_PATH"
    }

    suspend fun harTilgangTilBrukerMedOBO(
        personIdent: PersonIdent,
        token: String,
    ): Boolean {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = syfotilgangskontrollClientId,
            token = token
        )?.accessToken ?: throw RuntimeException("Failed to request access to Person: Failed to get OBO token")

        try {
            val response: HttpResponse = httpClient.get(tilgangskontrollPersonUrl) {
                header(NAV_PERSONIDENT_HEADER, personIdent.value)
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                accept(ContentType.Application.Json)
            }
            COUNT_TILGANGSKONTROLL_OK.increment()
            return response.body<TilgangDTO>().harTilgang
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                COUNT_TILGANGSKONTROLL_FORBIDDEN.increment()
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
        Metrics.counter(TILGANGSKONTROLL_FAIL, TAG_STATUS, statusCode).increment()
        return false
    }

    companion object {
        private val log = LoggerFactory.getLogger(TilgangskontrollConsumer::class.java)
        const val TILGANGSKONTROLL_PERSON_PATH = "/syfo-tilgangskontroll/api/tilgang/navident/person"
    }
}
