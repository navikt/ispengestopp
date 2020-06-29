package no.nav.syfo

import io.prometheus.client.Counter

const val METRICS_NS = "ispengestopp"


const val ENDRE_PERSON_STATUS_SUCCESS = "endre_person_status_success_count"

val COUNT_ENDRE_PERSON_STATUS_SUCCESS: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(ENDRE_PERSON_STATUS_SUCCESS)
        .help("Counts the number of successful posts to ispengestopp")
        .register()
