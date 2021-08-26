package no.nav.syfo.api.testutils

import io.ktor.application.*
import no.nav.syfo.StatusEndring
import no.nav.syfo.application.apiModule
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
) {
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        env = externalMockEnvironment.environment,
        personFlagget84Producer = personFlagget84Producer,
        wellKnownInternADV2 = externalMockEnvironment.wellKnownInternADV2Mock,
    )
}
