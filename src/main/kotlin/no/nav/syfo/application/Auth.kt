package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import no.nav.syfo.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.application.authentication")

fun Application.setupAuth(
    env: Environment,
    jwkProvider: JwkProvider
) {
    val jwtAudience = env.loginserviceClientId
    log.info("setup Authentication")
    install(Authentication) {
        jwt {
            verifier(jwkProvider, env.jwtIssuer)
            log.info("Done verifying")
            validate { credential ->
                log.info("Audience:" + credential.payload.audience.toString())
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    log.info("REJECTED!!! YOU'RE FIRED!")
                    null
                }
            }
        }
    }


}
