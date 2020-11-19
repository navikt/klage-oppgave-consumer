package no.nav.klage.oppgave.repositories

import no.nav.klage.oppgave.clients.FETCH_LIMIT
import no.nav.klage.oppgave.clients.OppgaveClient
import no.nav.klage.oppgave.domain.Oppgave
import no.nav.klage.oppgave.service.HjemmelParsingService
import org.springframework.stereotype.Component

@Component
class OppgaveRepository(
        private val oppgaveClient: OppgaveClient,
        private val hjemmelParsingService: HjemmelParsingService
) {
    fun bulkUpdateHjemmel() {
        var offset = 0

        val oppgaveResponse = oppgaveClient.fetchHjemmel(offset)

        val oppgaverWithNewHjemmel = setHjemmel(oppgaveResponse.oppgaver)

        oppgaverWithNewHjemmel.forEach { oppgaveClient.putOppgave(it) }

        offset += FETCH_LIMIT
    }

    private fun setHjemmel(oppgaver: List<Oppgave>): List<Oppgave> =
            oppgaver.mapNotNull {
                val possibleHjemmel = hjemmelParsingService.extractHjemmel(it.beskrivelse ?: "")
                if (possibleHjemmel.isEmpty()) {
                    null
                } else {
                    it.copy(
                            metadata = HashMap(it.metadata).apply {
                                put(OppgaveClient.HJEMMEL, possibleHjemmel[0])
                            }
                    )
                }
            }

}
