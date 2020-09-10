package no.nav.syfo.vault

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.database.VaultCredentialService
import no.nav.syfo.log

class RenewVaultService(private val vaultCredentialService: VaultCredentialService, private val applicationState: ApplicationState) {
    fun startRenewTasks() {
        GlobalScope.launch {
            try {
                Vault.renewVaultTokenTask(applicationState)
            } catch (e: Exception) {
                log.error("Noe gikk galt ved fornying av vault-token", e.message)
            } finally {
                applicationState.ready.set(false)
            }
        }

        GlobalScope.launch {
            try {
                vaultCredentialService.runRenewCredentialsTask(applicationState)
            } catch (e: Exception) {
                log.error("Noe gikk galt ved fornying av vault-credentials", e.message)
            } finally {
                applicationState.ready.set(false)
            }
        }
    }
}
