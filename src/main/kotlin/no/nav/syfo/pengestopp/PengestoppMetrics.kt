package no.nav.syfo.pengestopp

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Counter.builder
import no.nav.syfo.application.metric.METRICS_NS
import no.nav.syfo.application.metric.METRICS_REGISTRY

const val GET_PERSON_STATUS_FORBIDDEN = "${METRICS_NS}_get_person_status_forbidden_count"
const val ENDRE_PERSON_STATUS_DB_ALREADY_STORED = "${METRICS_NS}_endre_person_status_db_already_stored_count"

val COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED: Counter = builder(ENDRE_PERSON_STATUS_DB_ALREADY_STORED)
    .description("Counts the number of failed flaggings skipped because they are already stored in database")
    .register(METRICS_REGISTRY)

val COUNT_GET_PERSON_STATUS_FORBIDDEN: Counter = builder(GET_PERSON_STATUS_FORBIDDEN)
    .description("Counts the number of forbidden status checks")
    .register(METRICS_REGISTRY)
