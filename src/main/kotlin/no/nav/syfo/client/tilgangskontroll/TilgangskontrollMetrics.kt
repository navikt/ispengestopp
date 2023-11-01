package no.nav.syfo.client.tilgangskontroll

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Counter.builder
import no.nav.syfo.application.metric.METRICS_NS
import no.nav.syfo.application.metric.METRICS_REGISTRY

const val TILGANGSKONTROLL_OK = "${METRICS_NS}_tilgangskontroll_ok"
const val TILGANGSKONTROLL_FAIL = "${METRICS_NS}_tilgangskontroll_fail"
const val TILGANGSKONTROLL_FORBIDDEN = "${METRICS_NS}_call_tilgangskontroll_person_forbidden_count"

const val TAG_STATUS = "status"

val COUNT_TILGANGSKONTROLL_OK: Counter = builder(TILGANGSKONTROLL_OK)
    .description("Counts the number of successful requests to tilgangskontroll")
    .register(METRICS_REGISTRY)
val COUNT_TILGANGSKONTROLL_FORBIDDEN: Counter = builder(TILGANGSKONTROLL_FORBIDDEN)
    .description("Counts the number of forbidden calls to tilgangskontroll - person")
    .register(METRICS_REGISTRY)
