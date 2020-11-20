package no.nav.klage.oppgave.service

import no.nav.klage.oppgave.clients.FETCH_LIMIT
import no.nav.klage.oppgave.clients.HJEMMEL
import no.nav.klage.oppgave.clients.OppgaveClient
import no.nav.klage.oppgave.domain.Oppgave
import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import org.springframework.stereotype.Service

@Service
class OppgaveService(
        private val oppgaveClient: OppgaveClient,
        private val hjemmelParsingService: HjemmelParsingService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun bulkUpdateHjemmel() {
        val oppgaveList = fetchOppgaver()
        val oppgaverWithNewHjemmel = setHjemmel(oppgaveList)
        val oppgaverSuccessfullyPut = putOppgaver(oppgaverWithNewHjemmel)

        logger.info("Tried to put ${oppgaveList.size} oppgaver with $oppgaverSuccessfullyPut successful")
    }

    private fun putOppgaver(oppgaverList: List<Oppgave>): Int {
        var oppgaverSuccessfullyPut = 0

        oppgaverList.forEach { oppg ->
            kotlin.runCatching {
                oppgaveClient.putOppgave(oppg)
            }.onSuccess {
                oppgaverSuccessfullyPut++
            }.onFailure {
                logger.debug("Failed to put oppgave ${oppg.id}, just moving along.", it)
            }
        }

        return oppgaverSuccessfullyPut
    }

    private fun fetchOppgaver(): List<Oppgave> {
        var offset = 0

        var oppgaveResponse = oppgaveClient.fetchHjemmel(offset)

        val alleOppgaver = mutableListOf<Oppgave>()

        while (oppgaveResponse.oppgaver.isNotEmpty()) {
            alleOppgaver.addAll(oppgaveResponse.oppgaver)
            offset += FETCH_LIMIT
            oppgaveResponse = oppgaveClient.fetchHjemmel(offset)
        }

        return alleOppgaver
    }

    private fun setHjemmel(oppgaver: List<Oppgave>): List<Oppgave> =
            oppgaver.filter {
                it.metadata?.get(HJEMMEL) == null
            }.mapNotNull {
                val possibleHjemmel = hjemmelParsingService.extractHjemmel(it.beskrivelse ?: "")
                if (possibleHjemmel.isEmpty()) {
                    null
                } else {
                    it.copy(
                            metadata = HashMap(it.metadata).apply {
                                put(HJEMMEL, possibleHjemmel[0])
                            }
                    )
                }
            }

}
