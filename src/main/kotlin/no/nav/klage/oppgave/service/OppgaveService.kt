package no.nav.klage.oppgave.service

import no.nav.klage.oppgave.clients.FETCH_LIMIT
import no.nav.klage.oppgave.clients.HJEMMEL
import no.nav.klage.oppgave.clients.KlageDittnavAPIClient
import no.nav.klage.oppgave.clients.OppgaveClient
import no.nav.klage.oppgave.domain.*
import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveAPIClient: KlageDittnavAPIClient,
    private val hjemmelParsingService: HjemmelParsingService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun bulkUpdateHjemmel(request: BatchUpdateRequest): BatchUpdateResponse {
        val oppgaveList = if (request.oppgaveId != null) {
            listOf(oppgaveClient.getOppgave(request.oppgaveId))
        } else {
            fetchOppgaverWithoutHjemmel(request.includeFrom)
        }

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
            message = "$oppgaverSuccessfullyPut stored out of ${oppgaveList.size} with ${oppgaverWithNewHjemmel.size} new hjemler"
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

    private fun fetchOppgaverWithKlage(includeFrom: LocalDate?): List<Oppgave> {
        var offset = 0

        var oppgaveResponse = oppgaveClient.fetchAllKlageOppgaverForSYKBasedOnDate(includeFrom, offset)

        val alleOppgaver = mutableListOf<Oppgave>()

        while (oppgaveResponse.oppgaver.isNotEmpty()) {
            alleOppgaver += oppgaveResponse.oppgaver
            offset += FETCH_LIMIT
            oppgaveResponse = oppgaveClient.fetchAllKlageOppgaverForSYKBasedOnDate(includeFrom, offset)
        }

        return alleOppgaver
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
                    secureLogger.debug(
                        "Extracted hjemmel {} from beskrivelse {}",
                        possibleHjemmel.first(),
                        oppg.beskrivelse
                    )
                }
            }
        }

    fun storeLocalCopy(oppgave: OppgaveKafkaRecord) {
        oppgaveAPIClient.storeOppgave(oppgave)
    }

    fun batchStore(batchStoreRequest: BatchStoreRequest): BatchStoreResponse {
        val oppgaver = fetchOppgaverWithKlage(batchStoreRequest.includeFrom)

        var countOK = 0

        if (!batchStoreRequest.dryRun) {
            oppgaver.forEach { oppgave ->
                runCatching {
                    oppgaveAPIClient.storeOppgave(toOppgaveKafkaRecord(oppgave))
                    countOK++
                }.onFailure { throwable ->
                    logger.debug("Failed to store oppgave with id {}. See more in secure log", oppgave.id)
                    secureLogger.warn("Failed to store oppgave $oppgave", throwable)
                }
            }
        }

        val message = "Found ${oppgaver.size} oppgaver and managed to store $countOK"
        logger.debug(message)

        return BatchStoreResponse(message)
    }

    private fun toOppgaveKafkaRecord(oppgave: Oppgave): OppgaveKafkaRecord {
        return OppgaveKafkaRecord(
            id = oppgave.id,
            versjon = oppgave.versjon,
            journalpostId = oppgave.journalpostId,
            saksreferanse = oppgave.saksreferanse,
            mappeId = oppgave.mappeId,
            status = OppgaveKafkaRecord.Status.valueOf(
                oppgave.status?.name ?: throw RuntimeException("missing status")
            ),
            tildeltEnhetsnr = oppgave.tildeltEnhetsnr ?: "missing",
            opprettetAvEnhetsnr = oppgave.opprettetAvEnhetsnr,
            endretAvEnhetsnr = oppgave.endretAvEnhetsnr,
            tema = oppgave.tema,
            temagruppe = oppgave.temagruppe,
            behandlingstema = oppgave.behandlingstema,
            oppgavetype = oppgave.oppgavetype ?: "missing",
            behandlingstype = oppgave.behandlingstype,
            prioritet = OppgaveKafkaRecord.Prioritet.valueOf(
                oppgave.prioritet?.name ?: throw RuntimeException("missing prioritet")
            ),
            tilordnetRessurs = oppgave.tilordnetRessurs,
            beskrivelse = oppgave.beskrivelse,
            fristFerdigstillelse = oppgave.fristFerdigstillelse,
            aktivDato = LocalDate.parse(oppgave.aktivDato),
            opprettetAv = oppgave.opprettetAv ?: "missing opprettetAv",
            endretAv = oppgave.endretAv,
            opprettetTidspunkt = oppgave.opprettetTidspunkt ?: throw RuntimeException("missing opprettetTidspunkt"),
            endretTidspunkt = oppgave.endretTidspunkt,
            ferdigstiltTidspunkt = oppgave.ferdigstiltTidspunkt,
            behandlesAvApplikasjon = oppgave.behandlesAvApplikasjon,
            journalpostkilde = oppgave.journalpostkilde,
            ident = getIdent(oppgave.identer),
            metadata = toMetadata(oppgave.metadata)
        )
    }

    private fun toMetadata(metadata: Map<String, String>?): Map<OppgaveKafkaRecord.MetadataKey, String>? {
        return metadata?.map { (k, v) ->
            OppgaveKafkaRecord.MetadataKey.valueOf(k) to v
        }?.toMap()
    }

    private fun getIdent(identer: List<Ident>?): OppgaveKafkaRecord.Ident {
        if (identer != null) {
            val folkeregisterIdent = identer.find { it.gruppe == Gruppe.FOLKEREGISTERIDENT }
            val aktoerIdIdent = identer.find { it.gruppe == Gruppe.AKTOERID }
            return OppgaveKafkaRecord.Ident(
                identType = OppgaveKafkaRecord.IdentType.AKTOERID,
                verdi = aktoerIdIdent?.ident ?: "missing aktoerId",
                folkeregisterident = folkeregisterIdent?.ident
            )
        }

        throw RuntimeException("missing ident")
    }

}
