package no.nav.syfo.application

import no.nav.syfo.pengestopp.StatusEndring

interface IStatusEndringProducer {
    fun send(statusEndring: StatusEndring)
}
