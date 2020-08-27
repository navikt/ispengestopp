package no.nav.syfo.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.*
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.Flaggperson84Kt")

fun Route.registerFlaggPerson84(
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    tilgangskontroll: TilgangskontrollConsumer
) {
    route("/api/v1") {
        get("/person/status") {
            log.info("Received get request to /api/v1/person/status")

            if (call.request.headers.contains("fnr") == false) call.respond(HttpStatusCode.BadRequest)
            val sykmeldtFnr = SykmeldtFnr(call.request.headers["fnr"]!!)

            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val hasAccess = tilgangskontroll.harTilgangTilBruker(sykmeldtFnr, token!!)
            if (hasAccess) {
                println("Get active flags for sykmeldt")
                val flags: List<StatusEndring> = database.getActiveFlags(sykmeldtFnr)
                when {
                    flags.isNotEmpty() -> call.respond(flags)
                    else -> call.respond(HttpStatusCode.NoContent)
                }
            } else {
                COUNT_GET_PERSON_STATUS_FORBIDDEN.inc()
                call.respond(HttpStatusCode.Forbidden)
            }
        }

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
                    val kFlaggperson84Hendelse = StatusEndring(
                        stoppAutomatikk.veilederIdent,
                        stoppAutomatikk.sykmeldtFnr,
                        Status.STOPP_AUTOMATIKK,
                        it,
                        OffsetDateTime.now(ZoneOffset.UTC),
                        stoppAutomatikk.enhetNr
                    )

                    personFlagget84Producer.send(
                        ProducerRecord(
                            env.stoppAutomatikkTopic,
                            "${stoppAutomatikk.sykmeldtFnr}-$it",
                            kFlaggperson84Hendelse
                        )
                    )

                    log.info("Lagt melding på kafka: Topic: {}", env.stoppAutomatikkTopic)
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
