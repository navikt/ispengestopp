package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.IPengestoppRepository
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollClient
import no.nav.syfo.client.wellknown.WellKnown
import no.nav.syfo.pengestopp.StatusEndring
import no.nav.syfo.pengestopp.api.registerFlaggPerson84V2
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    pengestoppRepository: IPengestoppRepository,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    wellKnownInternADV2: WellKnown,
    tilgangskontrollClient: TilgangskontrollClient,
) {
    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(env.azureAppClientId),
                jwtIssuerType = JwtIssuerType.INTERN_AZUREAD_V2,
                wellKnown = wellKnownInternADV2,
            ),
        )
    )
    installCallId()
    installContentNegotiation()
    installMetrics()
    installStatusPages()

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerMetricApi()
        authenticate(JwtIssuerType.INTERN_AZUREAD_V2.name) {
            registerFlaggPerson84V2(
                pengestoppRepository = pengestoppRepository,
                env,
                personFlagget84Producer,
                tilgangskontrollClient
            )
        }
    }
}
