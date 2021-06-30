package no.nav.syfo.api.testutils

import io.ktor.server.netty.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.api.testutils.mock.wellKnownMock
import no.nav.syfo.application.ApplicationState

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDB()
    val embeddedEnvironment: KafkaEnvironment = testKafka()

    val tilgangskontrollMock = VeilederTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        tilgangskontrollMock.name to tilgangskontrollMock.server,
    )

    val environment = testEnvironment(
        kafkaBrokersURL = embeddedEnvironment.brokersURL,
        syfotilgangskontrollUrl = tilgangskontrollMock.url,
    )

    val wellKnownInternADMock = wellKnownMock()
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.externalApplicationMockMap.start()
    this.embeddedEnvironment.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.externalApplicationMockMap.stop()
    this.database.stop()
    this.embeddedEnvironment.tearDown()
}

fun HashMap<String, NettyApplicationEngine>.start() {
    this.forEach {
        it.value.start()
    }
}

fun HashMap<String, NettyApplicationEngine>.stop(
    gracePeriodMillis: Long = 1L,
    timeoutMillis: Long = 10L
) {
    this.forEach {
        it.value.stop(gracePeriodMillis, timeoutMillis)
    }
}
