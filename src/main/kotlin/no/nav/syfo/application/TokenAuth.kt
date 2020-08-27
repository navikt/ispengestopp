package no.nav.syfo.application

import com.auth0.jwt.JWT
import no.nav.syfo.VeilederIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun getVeilederIdentFromToken(token: String): VeilederIdent {
    val decodedJWT = JWT.decode(token)
    val navIdent: String = decodedJWT.claims["NAVident"]?.asString()
        ?: throw Error("Missing NAVident in private claims")
    return VeilederIdent(navIdent)
}

