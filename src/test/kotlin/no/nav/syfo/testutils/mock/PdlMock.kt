package no.nav.syfo.testutils.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.pdl.*
import no.nav.syfo.testutils.UserConstants

suspend fun MockRequestHandleScope.pdlMockResponse(request: HttpRequestData): HttpResponseData {
    val pdlRequest = request.receiveBody<PdlHentIdenterRequest>()
    return when (pdlRequest.variables.ident) {
        UserConstants.SYKMELDT_PERSONIDENT_3.value -> {
            respond(generatePdlIdenter("enAnnenIdent"))
        }

        else -> {
            respond(generatePdlIdenter(pdlRequest.variables.ident))
        }
    }
}

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
