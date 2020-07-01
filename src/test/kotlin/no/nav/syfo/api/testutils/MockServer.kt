package no.nav.syfo.api.testutils

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.syfo.SykmeldtFnr
import no.nav.syfo.Tilgang


fun mockSyfotilgangskontrollServer(port: Int, fnr: SykmeldtFnr): ApplicationEngine {
    return embeddedServer(Netty, port) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        routing {
            get("/syfo-tilgangskontroll/api/tilgang/bruker") {
                if (call.request.queryParameters["fnr"]!! == fnr.value) {
                    call.respond(Tilgang(true))
                } else {
                    call.respond(Tilgang(false, "Vil ikke"))
                }
            }
        }
    }
}
