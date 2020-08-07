package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.syfo.*
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.Flaggperson84Kt")

fun Route.registerFlaggPerson84(
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, KFlaggperson84Hendelse>,
    tilgangskontroll: TilgangskontrollConsumer
) {
    route("/api/v1") {
        post("/person/flagg") {
            val stoppAutomatikk: StoppAutomatikk = call.receive()
            log.info("Received post request to /api/v1/person/flagg")

            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")

            if (token == null) {
                call.respond(HttpStatusCode.Forbidden)
            }

            val harTilgang = tilgangskontroll.harTilgangTilBruker(stoppAutomatikk.sykmeldtFnr, token!!)

            if (harTilgang) {
                stoppAutomatikk.virksomhetNr.forEach {
                    val kFlaggperson84Hendelse = KFlaggperson84Hendelse(
                        stoppAutomatikk.veilederIdent,
                        stoppAutomatikk.sykmeldtFnr,
                        Status.STOPP_AUTOMATIKK,
                        it,
                        LocalDateTime.now(),
                        stoppAutomatikk.enhetNr
                    )

                    personFlagget84Producer.send(
                        ProducerRecord(
                            env.flaggPerson84Topic,
                            "${stoppAutomatikk.sykmeldtFnr}-$it",
                            kFlaggperson84Hendelse
                        )
                    )

                    log.info("Lagt melding på kafka: Topic: {}", env.flaggPerson84Topic)

                    database.addFlagg(
                        stoppAutomatikk.sykmeldtFnr,
                        stoppAutomatikk.veilederIdent,
                        stoppAutomatikk.enhetNr,
                        it
                    )
                }
                COUNT_ENDRE_PERSON_STATUS_SUCCESS.inc()
                call.respond(HttpStatusCode.Created)
            } else {
                COUNT_ENDRE_PERSON_STATUS_FORBIDDEN.inc()
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}
