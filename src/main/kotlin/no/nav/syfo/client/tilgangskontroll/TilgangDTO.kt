package no.nav.syfo.client.tilgangskontroll

data class TilgangDTO(
    val harTilgang: Boolean,
    val begrunnelse: String? = null,
)
