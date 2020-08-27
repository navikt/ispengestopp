package no.nav.syfo.util

fun nrValidator(value: String) {
    if (value.isBlank()) {
        throw IllegalArgumentException("Nr cannot be empty")
    }
    try {
        Integer.parseInt(value)
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Nr must be numerical")

    }
}
