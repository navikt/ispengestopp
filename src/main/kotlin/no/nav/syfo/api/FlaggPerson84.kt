package no.nav.syfo.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.*
import no.nav.syfo.application.getVeilederIdentFromToken
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.util.callIdArgument
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.Flaggperson84Kt")

const val apiBasePath = "/api/v1"
const val apiPersonStatusPath = "/person/status"
const val apiPersonFlaggPath = "/person/flagg"

fun Route.registerFlaggPerson84(
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    tilgangskontroll: TilgangskontrollConsumer
) {
    route(apiBasePath) {
        get(apiPersonStatusPath) {
            log.info("Received get request to $apiBasePath$apiPersonStatusPath")

            val callId = getCallId()

            try {
                val requestFnr = call.request.headers["fnr"] ?: throw IllegalArgumentException("No Fnr supplied")
                val token = getBearerHeader() ?: throw IllegalArgumentException("No Authorization header supplied")

                val sykmeldtFnr = SykmeldtFnr(requestFnr)
                val hasAccess = tilgangskontroll.harTilgangTilBruker(sykmeldtFnr, token)
                if (hasAccess) {
                    val flags: List<StatusEndring> = database.getActiveFlags(sykmeldtFnr)
                    when {
                        flags.isNotEmpty() -> call.respond(flags)
                        else -> call.respond(HttpStatusCode.NoContent)
                    }
                } else {
                    COUNT_GET_PERSON_STATUS_FORBIDDEN.inc()
                    call.respond(HttpStatusCode.Forbidden)
                }
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Request to get for PersonIdent"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callIdArgument(callId))
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }

        post(apiPersonFlaggPath) {
            log.info("Received post request to $apiBasePath$apiPersonStatusPath")

            val callId = getCallId()

            val token = getBearerHeader() ?: throw IllegalArgumentException("No Authorization header supplied")
            try {
                val stoppAutomatikk: StoppAutomatikk = call.receive()
                val ident = getVeilederIdentFromToken(token)
                val harTilgang = tilgangskontroll.harTilgangTilBruker(stoppAutomatikk.sykmeldtFnr, token)
                if (harTilgang) {
                    stoppAutomatikk.virksomhetNr.forEach {
                        val kFlaggperson84Hendelse = StatusEndring(
                            UUID.randomUUID().toString(),
                            ident,
                            stoppAutomatikk.sykmeldtFnr,
                            Status.STOPP_AUTOMATIKK,
                            stoppAutomatikk.arsakList,
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
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Request to post StatusEndring failed"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callIdArgument(callId))
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
