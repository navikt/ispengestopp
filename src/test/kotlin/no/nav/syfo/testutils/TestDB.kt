package no.nav.syfo.testutils

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.application.database.DatabaseInterface
import org.flywaydb.core.Flyway
import java.sql.Connection

class TestDB : DatabaseInterface {

    private val pg: EmbeddedPostgres

    override val connection: Connection
        get() = pg.postgresDatabase.connection.apply {
            autoCommit = false
        }

    init {
        pg = try {
            EmbeddedPostgres.start()
        } catch (e: Exception) {
            EmbeddedPostgres.builder().setLocaleConfig("locale", "en_US").start()
        }

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).load().migrate()
        }
    }

    fun stop() {
        pg.close()
    }
}

fun Connection.dropData() {
    val queryList = listOf(
        """
        DELETE FROM status_endring
        """.trimIndent(),
        """
        DELETE FROM ARSAK
        """.trimIndent(),
    )
    this.use { connection ->
        queryList.forEach { query ->
            connection.prepareStatement(query).execute()
        }
        connection.commit()
    }
}
