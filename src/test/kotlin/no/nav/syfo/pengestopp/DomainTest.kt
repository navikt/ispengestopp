package no.nav.syfo.pengestopp

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DomainTest {

    @Nested
    @DisplayName("VirksomhetsNr")
    inner class VirksomhetsNrTest {

        @Test
        fun `Should not be non-numerical`() {
            assertThrows<IllegalArgumentException> {
                VirksomhetNr("ABC")
            }
        }

        @Test
        fun `Should not be empty`() {
            assertThrows<IllegalArgumentException> {
                VirksomhetNr("")
            }
        }
    }

    @Nested
    @DisplayName("EnhetNr")
    inner class EnhetNrTest {

        @Test
        fun `Should not be non-numerical`() {
            assertThrows<IllegalArgumentException> {
                EnhetNr("ABC")
            }
        }

        @Test
        fun `Should not be empty`() {
            assertThrows<IllegalArgumentException> {
                EnhetNr("")
            }
        }
    }
}
