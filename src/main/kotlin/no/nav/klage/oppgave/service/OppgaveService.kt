package no.nav.klage.oppgave.service

import no.nav.klage.oppgave.clients.FETCH_LIMIT
import no.nav.klage.oppgave.clients.HJEMMEL
import no.nav.klage.oppgave.clients.OppgaveClient
import no.nav.klage.oppgave.domain.BatchUpdateRequest
import no.nav.klage.oppgave.domain.BatchUpdateResponse
import no.nav.klage.oppgave.domain.Oppgave
import no.nav.klage.oppgave.domain.ResponseStatus
import no.nav.klage.oppgave.utils.getLogger
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(
        private val oppgaveClient: OppgaveClient,
        private val hjemmelParsingService: HjemmelParsingService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun bulkUpdateHjemmel(request: BatchUpdateRequest): BatchUpdateResponse {
        val oppgaveList = fetchOppgaverWithoutHjemmel()
        val oppgaverWithNewHjemmel = setHjemmel(oppgaveList)
        val oppgaverSuccessfullyPut = if (!request.dryRun) {
            putOppgaver(oppgaverWithNewHjemmel)
        } else {
            0
        }

        logger.info("Tried to put {} oppgaver with {} successful", oppgaveList.size, oppgaverSuccessfullyPut)
        val status: ResponseStatus = when(oppgaverSuccessfullyPut) {
            oppgaveList.size -> ResponseStatus.OK
            else -> ResponseStatus.PARTIAL
        }
        return BatchUpdateResponse(
                finished = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                status = status,
                message = "$oppgaverSuccessfullyPut stored out og ${oppgaveList.size}"
        )
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

    private fun fetchOppgaverWithoutHjemmel(): List<Oppgave> {
        var offset = 0

        var oppgaveResponse = oppgaveClient.fetchOppgaver(offset)

        val alleOppgaver = mutableListOf<Oppgave>()

        while (oppgaveResponse.oppgaver.isNotEmpty()) {
            alleOppgaver += oppgaveResponse.oppgaver
            offset += FETCH_LIMIT
            oppgaveResponse = oppgaveClient.fetchOppgaver(offset)
        }

        return alleOppgaver.filter {
            it.metadata?.get(HJEMMEL) == null
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
                                put(HJEMMEL, possibleHjemmel.first())
                            }
                    )
                }
            }

}
