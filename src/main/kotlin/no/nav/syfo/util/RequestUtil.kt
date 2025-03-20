
package no.nav.syfo.util

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import net.logstash.logback.argument.StructuredArguments

const val NAV_PERSONIDENT_HEADER = "nav-personident"

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"

fun RoutingContext.getCallId(): String = this.call.getCallId()

fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"

fun ApplicationCall.getConsumerId(): String = this.request.headers[NAV_CONSUMER_ID_HEADER].toString()

fun RoutingContext.getBearerHeader(): String? =
    this.call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
