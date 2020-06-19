package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.syfo.StoppAutomatikk
import no.nav.syfo.addFlagg
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.log

// TODO det virker som at denne ikke kompileres som en extension, men som en metode med Route som argument :(
// TODO Eller, kanskje problemet er at det ikke blir gjort? D:
// Sånn jeg skjønner det, skal denne metoden kompileres til FlaggPerson84Kt.registerFlaggPerson84(Route route, DatabaseInterface database) {kode}
// Og det er vel det testen vår forsøker å kalle, men så skjer det kanskje noe feil ved kompilering som gjør at den metoden ikke finnes.
// Da tipper jeg det kan ha noe med klasse-/pakke-/filnavn å gjøre
// Men jeg skjønner likevel ikke hvorfor det funket før, men ikke nå :( :( SAD!!!!
fun Route.registerFlaggPerson84(
    database: DatabaseInterface
) {
    route("/api/v1") {
        post("/person/flagg") {
            val stoppAutomatikk: StoppAutomatikk = call.receive()
            log.info("Recived call to /api/v1/person/flagg")
            log.info("Body $stoppAutomatikk")

            stoppAutomatikk.virksomhetNr.forEach {
                database.addFlagg(stoppAutomatikk.sykmeldtFnr, stoppAutomatikk.veilederIdent, it)
            }

            call.respondText("{}", status = HttpStatusCode.OK)
        }
    }
}
