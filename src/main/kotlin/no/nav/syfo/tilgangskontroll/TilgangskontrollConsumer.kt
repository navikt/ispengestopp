package no.nav.syfo.tilgangskontroll

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.COUNT_TILGANGSKONTROLL_FAIL
import no.nav.syfo.COUNT_TILGANGSKONTROLL_OK
import no.nav.syfo.SykmeldtFnr
import no.nav.syfo.Tilgang
import org.slf4j.LoggerFactory

class TilgangskontrollConsumer(private val url: String = "http://syfo-tilgangskontroll/syfo-tilgangskontroll/api/tilgang/bruker") {
    val log = LoggerFactory.getLogger("no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer")

    @KtorExperimentalAPI
    private val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = GsonSerializer {
                serializeNulls()
                disableHtmlEscaping()
            }
        }
    }

    suspend fun harTilgangTilBruker(fnr: SykmeldtFnr, token: String): Boolean {
        val response: HttpResponse = httpClient.get("$url?fnr=${fnr.value}") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                COUNT_TILGANGSKONTROLL_OK.inc()
                response.receive<Tilgang>().harTilgang
            }
            else -> {
                val statusCode: String = response.status.value.toString()
                COUNT_TILGANGSKONTROLL_FAIL.labels(statusCode).inc()
                log.info("Ingen tilgang, Tilgangskontroll returnerte Status : {}", response.status)
                false
            }
        }
    }
}
