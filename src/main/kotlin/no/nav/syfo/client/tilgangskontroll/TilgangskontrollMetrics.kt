package no.nav.syfo

import io.prometheus.client.Counter
import no.nav.syfo.application.metric.METRICS_NS

const val TILGANGSKONTROLL_OK = "tilgangskontroll_ok"
const val TILGANGSKONTROLL_FAIL = "tilgangskontroll_fail"

val COUNT_TILGANGSKONTROLL_OK: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(TILGANGSKONTROLL_OK)
    .help("Counts the number of successful requests to tilgangskontroll")
    .register()

val COUNT_TILGANGSKONTROLL_FAIL: Counter = Counter.build()
    .namespace(METRICS_NS)
    .labelNames("status")
    .name(TILGANGSKONTROLL_FAIL)
    .help("Counts the number of failing requests to tilgangskontroll")
    .register()
const val TILGANGSKONTROLL_FORBIDDEN = "call_tilgangskontroll_person_forbidden_count"
val COUNT_TILGANGSKONTROLL_FORBIDDEN: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(TILGANGSKONTROLL_FORBIDDEN)
    .help("Counts the number of forbidden calls to syfo-tilgangskontroll - person")
    .register()
