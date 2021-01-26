package no.nav.syfo.api.testutils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.*
import io.ktor.jackson.*
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
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        routing {
            get("/syfo-tilgangskontroll/api/tilgang/bruker") {
                if (call.request.queryParameters["fnr"]!! == fnr.value) {
                    call.respond(Tilgang(true))
                } else {
                    call.respond(HttpStatusCode.Forbidden, Tilgang(false, "Vil ikke"))
                }
            }
        }
    }
}
