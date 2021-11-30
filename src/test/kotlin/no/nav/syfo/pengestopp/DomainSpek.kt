package no.nav.syfo.pengestopp

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFailsWith

object DomainSpek : Spek({
    describe("VirksomhetsNr") {
        it("Should not be non-numerical") {
            assertFailsWith(IllegalArgumentException::class) {
                VirksomhetNr("ABC")
            }
        }
        it("Should not be empty") {
            assertFailsWith(IllegalArgumentException::class) {
                VirksomhetNr("")
            }
        }
    }
    describe("EnhetNr") {
        it("Should not be non-numerical") {
            assertFailsWith(IllegalArgumentException::class) {
                EnhetNr("ABC")
            }
        }
        it("Should not be empty") {
            assertFailsWith(IllegalArgumentException::class) {
                EnhetNr("")
            }
        }
    }
})
