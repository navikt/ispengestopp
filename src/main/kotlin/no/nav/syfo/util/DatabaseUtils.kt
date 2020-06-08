package no.nav.syfo.util

import org.postgresql.util.PGobject

fun String.toPGObject() = PGobject().also {
    it.type = "json"
    it.value = this
}
