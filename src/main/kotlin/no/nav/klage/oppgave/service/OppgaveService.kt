package no.nav.klage.oppgave.service

import no.nav.klage.oppgave.clients.FETCH_LIMIT
import no.nav.klage.oppgave.clients.OppgaveClient
import no.nav.klage.oppgave.domain.Oppgave
import org.springframework.stereotype.Service

@Service
class OppgaveService(
        private val oppgaveClient: OppgaveClient,
        private val hjemmelParsingService: HjemmelParsingService
) {
    fun bulkUpdateHjemmel() {
        var offset = 0

        var oppgaveResponse = oppgaveClient.fetchHjemmel(offset)

        while (oppgaveResponse.antallTreffTotalt > 0) {
            val oppgaverWithNewHjemmel = setHjemmel(oppgaveResponse.oppgaver)

            oppgaverWithNewHjemmel.forEach { oppgaveClient.putOppgave(it) }

            offset += FETCH_LIMIT

            oppgaveResponse = oppgaveClient.fetchHjemmel(offset)
        }
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
