package no.nav.klage.oppgave.repositories

import no.nav.klage.oppgave.utils.getLogger
import org.springframework.stereotype.Repository

@Repository
class OppgaveRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun storeHjemmelInMetadata(oppgaveId: Long, hjemmel: String) {
        logger.debug("'hardcoded dry run': storeHjemmelInMetadata. OppgaveId: {}, hjemmel: {}", oppgaveId, hjemmel)
    }
}
// Fetch batch

// Store batch
