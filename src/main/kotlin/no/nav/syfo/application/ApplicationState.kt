package no.nav.syfo.application

import java.util.concurrent.atomic.AtomicBoolean

data class ApplicationState(
    var alive: AtomicBoolean = AtomicBoolean(true),
    var ready: AtomicBoolean = AtomicBoolean(false)
)
