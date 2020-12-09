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
import java.time.LocalDate
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
        val oppgaveList = fetchOppgaverWithoutHjemmel(request.includeFrom)
        val oppgaverWithNewHjemmel = setHjemmel(oppgaveList)

        logger.info("Set hjemmel on {} oppgaver out of {}", oppgaverWithNewHjemmel.size, oppgaveList.size)

        val oppgaverSuccessfullyPut = if (!request.dryRun) {
            putOppgaver(oppgaverWithNewHjemmel)
        } else {
            0
        }

        logger.info("Tried to put {} oppgaver with {} successful", oppgaveList.size, oppgaverSuccessfullyPut)
        val status: ResponseStatus = when (oppgaverSuccessfullyPut) {
            oppgaveList.size -> ResponseStatus.OK
            else -> ResponseStatus.PARTIAL
        }
        return BatchUpdateResponse(
            finished = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            status = status,
            message = "$oppgaverSuccessfullyPut stored out of ${oppgaveList.size}"
        )
    }

    fun updateHjemmel(oppgaveId: Long, hjemmel: String) {
        logger.debug("Update hjemmel. OppgaveId: {}, hjemmel: {}", oppgaveId, hjemmel)
        val oppgave = oppgaveClient.getOppgave(oppgaveId)
        oppgaveClient.putOppgave(
            oppgave.copy(
                metadata = HashMap(oppgave.metadata).apply {
                    put(HJEMMEL, hjemmel)
                }
            )
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

    private fun fetchOppgaverWithoutHjemmel(includeFrom: LocalDate?): List<Oppgave> {
        var offset = 0

        var oppgaveResponse = oppgaveClient.fetchOppgaver(includeFrom, offset)

        val alleOppgaver = mutableListOf<Oppgave>()

        while (oppgaveResponse.oppgaver.isNotEmpty()) {
            alleOppgaver += oppgaveResponse.oppgaver
            offset += FETCH_LIMIT
            oppgaveResponse = oppgaveClient.fetchOppgaver(includeFrom, offset)
        }

        return alleOppgaver.filter {
            it.metadata?.get(HJEMMEL) == null
        }
    }

    private fun setHjemmel(oppgaver: List<Oppgave>): List<Oppgave> =
        oppgaver.mapNotNull { oppg ->
            val possibleHjemmel = hjemmelParsingService.extractHjemmel(oppg.beskrivelse ?: "")
            if (possibleHjemmel.isEmpty()) {
                null
            } else {
                oppg.copy(
                    metadata = HashMap(oppg.metadata).apply {
                        put(HJEMMEL, possibleHjemmel.first())
                    }
                ).also {
                    logger.debug("Extracted hjemmel {} from beskrivelse {}", possibleHjemmel.first(), oppg.beskrivelse)
                }
            }
        }

}
