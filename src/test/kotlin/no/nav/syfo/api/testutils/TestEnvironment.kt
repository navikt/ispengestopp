package no.nav.syfo.api.testutils

import java.net.ServerSocket

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
