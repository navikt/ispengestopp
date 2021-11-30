package no.nav.syfo.client.azuread

import java.time.LocalDateTime

data class AzureAdV2TokenResponse(
    val access_token: String,
    val expires_in: Long,
    val token_type: String
)

fun AzureAdV2TokenResponse.toAzureAdV2Token(): AzureAdToken {
    val expiresOn = LocalDateTime.now().plusSeconds(this.expires_in)
    return AzureAdToken(
        accessToken = this.access_token,
        expires = expiresOn
    )
}
