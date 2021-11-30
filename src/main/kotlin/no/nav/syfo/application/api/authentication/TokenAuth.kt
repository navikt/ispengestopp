package no.nav.syfo.application.api.authentication

import com.auth0.jwt.JWT
import no.nav.syfo.pengestopp.VeilederIdent

const val JWT_CLAIM_NAVIDENT = "NAVident"

fun getVeilederIdentFromToken(token: String): VeilederIdent {
    val decodedJWT = JWT.decode(token)
    val navIdent: String = decodedJWT.claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
    return VeilederIdent(navIdent)
}
