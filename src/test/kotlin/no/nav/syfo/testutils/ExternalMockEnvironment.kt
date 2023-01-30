package no.nav.syfo.testutils

import io.ktor.server.netty.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testutils.mock.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDB()
    val embeddedEnvironment: KafkaEnvironment = testKafka()

    val azureAdV2Mock = AzureAdV2Mock()
    val tilgangskontrollMock = VeilederTilgangskontrollMock()
    val pdlMock = PdlMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdV2Mock.name to azureAdV2Mock.server,
        tilgangskontrollMock.name to tilgangskontrollMock.server,
    )

    val environment = testEnvironment(
        azureTokenEndpoint = azureAdV2Mock.url,
        kafkaBrokersURL = embeddedEnvironment.brokersURL,
        pdlUrl = pdlMock.url,
        syfotilgangskontrollUrl = tilgangskontrollMock.url,
    )

    val wellKnownInternADV2Mock = wellKnownInternADMock()
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
