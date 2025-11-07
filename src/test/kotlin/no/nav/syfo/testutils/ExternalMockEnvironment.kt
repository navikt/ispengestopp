package no.nav.syfo.testutils

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testutils.mock.mockHttpClient
import no.nav.syfo.testutils.mock.wellKnownInternADMock

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDB()

    val environment = testEnvironment()

    val mockHttpClient = mockHttpClient(environment = environment)

    val wellKnownInternADV2Mock = wellKnownInternADMock()

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.database.stop()
}
