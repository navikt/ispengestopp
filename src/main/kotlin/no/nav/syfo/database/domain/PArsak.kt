package no.nav.syfo.database.domain

import no.nav.syfo.Arsak
import no.nav.syfo.SykepengestoppArsak
import java.time.OffsetDateTime

data class PArsak(
    val id: Int,
    val uuid: String,
    val statusEndringId: Int,
    val arsakType: String,
    val opprettet: OffsetDateTime
)

fun PArsak.toArsak(): Arsak =
    Arsak(
        type = SykepengestoppArsak.valueOf(this.arsakType)
    )
