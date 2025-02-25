package no.nav.syfo.infrastructure.database

import no.nav.syfo.pengestopp.Arsak
import no.nav.syfo.pengestopp.SykepengestoppArsak
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
