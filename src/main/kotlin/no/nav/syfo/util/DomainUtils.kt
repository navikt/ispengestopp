package no.nav.syfo.util

fun nrValidator(value: String) {
    if (value.isBlank()) {
        throw IllegalArgumentException("VirksomhetNr cannot be empty")
    }
    try {
        Integer.parseInt(value)
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("VirksomhetNr must be numerical")

    }
}
