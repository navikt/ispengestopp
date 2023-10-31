package no.nav.syfo.testutils

import no.nav.common.KafkaEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testutils.mock.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDB()
    val embeddedEnvironment: KafkaEnvironment = testKafka()

    val environment = testEnvironment(
        kafkaBrokersURL = embeddedEnvironment.brokersURL,
    )

    val mockHttpClient = mockHttpClient(environment = environment)

    val wellKnownInternADV2Mock = wellKnownInternADMock()
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.embeddedEnvironment.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.database.stop()
    this.embeddedEnvironment.tearDown()
}
