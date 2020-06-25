package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import no.nav.syfo.Environment

fun Application.setupAuth(
    env: Environment,
    jwkProvider: JwkProvider
) {
    val jwtAudience = env.loginserviceClientId

    install(Authentication) {
        jwt {
            verifier(jwkProvider, env.jwtIssuer)
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }


}
