package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface

fun Routing.registerPodApi(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
) {
    get("/is_alive") {
        if (applicationState.alive.get()) {
            call.respondText("I'm alive! :)")
        } else {
            call.respondText("I'm dead x_x", status = HttpStatusCode.InternalServerError)
        }
    }
    get("/is_ready") {
        val isReady = isReady(
            applicationState = applicationState,
            database = database,
        )
        if (isReady) {
            call.respondText("I'm ready! :)")
        } else {
            call.respondText("Please wait! I'm not ready :(", status = HttpStatusCode.InternalServerError)
        }
    }
    get("/prometheus") {
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
        }
    }
}

private fun isReady(
    applicationState: ApplicationState,
    database: DatabaseInterface,
): Boolean {
    return applicationState.ready.get() && database.isDatabaseOK()
}

private fun DatabaseInterface.isDatabaseOK(): Boolean {
    return try {
        connection.use {
            it.isValid(1)
        }
    } catch (exc: Exception) {
        false
    }
}
