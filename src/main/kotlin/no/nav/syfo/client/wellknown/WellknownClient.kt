package no.nav.syfo.client.wellknown

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.httpClientProxy

val client = httpClientProxy()

fun getWellKnown(wellKnownUrl: String): WellKnown = runBlocking {
    client.get(wellKnownUrl).body<WellKnownDTO>().toWellKnown()
}
