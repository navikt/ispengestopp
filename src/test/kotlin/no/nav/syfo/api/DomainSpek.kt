package no.nav.syfo.api

import no.nav.syfo.EnhetNr
import no.nav.syfo.VirksomhetNr
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFailsWith

object DomainSpek : Spek({
    describe("VirksomhetsNr") {
        it("Should throw IllegalArgumentException") {
            assertFailsWith(IllegalArgumentException::class) {
                VirksomhetNr("ABC")
            }
        }

    }
    describe("EnhetNr") {
        it("Should throw IllegalArgumentException") {
            assertFailsWith(IllegalArgumentException::class) {
                EnhetNr("ABC")
            }
        }
    }

})
