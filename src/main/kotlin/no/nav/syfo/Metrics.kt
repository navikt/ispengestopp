package no.nav.syfo

import io.prometheus.client.Counter

const val METRICS_NS = "ispengestopp"

const val ENDRE_PERSON_STATUS_SUCCESS = "endre_person_status_success_count"
const val ENDRE_PERSON_STATUS_FORBIDDEN = "endre_person_status_forbidden_count"
const val GET_PERSON_STATUS_FORBIDDEN = "get_person_status_forbidden_count"
const val ENDRE_PERSON_STATUS_DB_FAILED = "endre_person_status_db_failed_count"
const val ENDRE_PERSON_STATUS_DB_ALREADY_STORED = "endre_person_status_db_already_stored_count"

const val TILGANGSKONTROLL_OK = "tilgangskontroll_ok"
const val TILGANGSKONTROLL_FAIL = "tilgangskontroll_fail"

val COUNT_ENDRE_PERSON_STATUS_SUCCESS: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(ENDRE_PERSON_STATUS_SUCCESS)
    .help("Counts the number of successful posts to ispengestopp")
    .register()

val COUNT_ENDRE_PERSON_STATUS_FORBIDDEN: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(ENDRE_PERSON_STATUS_FORBIDDEN)
    .help("Counts the number of forbidden flaggings")
    .register()

val COUNT_ENDRE_PERSON_STATUS_DB_FAILED: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(ENDRE_PERSON_STATUS_DB_FAILED)
    .help("Counts the number of failed saves of flaggings to database")
    .register()

val COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(ENDRE_PERSON_STATUS_DB_ALREADY_STORED)
    .help("Counts the number of failed flaggings skipped because they are already stored in database")
    .register()

val COUNT_GET_PERSON_STATUS_FORBIDDEN: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(GET_PERSON_STATUS_FORBIDDEN)
    .help("Counts the number of forbidden status checks")
    .register()

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
