package no.nav.klage.oppgave.facades

import no.nav.klage.oppgave.domain.*
import no.nav.klage.oppgave.domain.oppgavekopi.*
import no.nav.klage.oppgave.domain.oppgavekopi.Ident
import no.nav.klage.oppgave.domain.oppgavekopi.Prioritet
import no.nav.klage.oppgave.domain.oppgavekopi.Status
import no.nav.klage.oppgave.services.OppgaveApiService
import no.nav.klage.oppgave.services.OppgaveKopiService
import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OppgaveFacade(
    private val oppgaveKopiService: OppgaveKopiService,
    private val oppgaveApiService: OppgaveApiService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun updateHjemmel(oppgaveId: Long, hjemmel: String) {
        oppgaveApiService.updateHjemmel(oppgaveId, hjemmel)
    }

    fun saveOppgaveKopi(oppgave: OppgaveKafkaRecord) {
        oppgaveKopiService.saveOppgaveKopi(mapOppgaveKafkaRecordToOppgaveKopi(oppgave))
    }

    fun mapOppgaveKafkaRecordToOppgaveKopi(oppgave: OppgaveKafkaRecord): OppgaveKopi {
        return OppgaveKopi(
            id = oppgave.id,
            versjon = oppgave.versjon,
            journalpostId = oppgave.journalpostId,
            saksreferanse = oppgave.saksreferanse,
            mappeId = oppgave.mappeId,
            status = Status.valueOf(oppgave.status.name),
            tildeltEnhetsnr = oppgave.tildeltEnhetsnr,
            opprettetAvEnhetsnr = oppgave.opprettetAvEnhetsnr,
            endretAvEnhetsnr = oppgave.endretAvEnhetsnr,
            tema = oppgave.tema,
            temagruppe = oppgave.temagruppe,
            behandlingstema = oppgave.behandlingstema,
            oppgavetype = oppgave.oppgavetype,
            behandlingstype = oppgave.behandlingstype,
            prioritet = Prioritet.valueOf(oppgave.prioritet.name),
            tilordnetRessurs = oppgave.tilordnetRessurs,
            beskrivelse = oppgave.beskrivelse,
            fristFerdigstillelse = oppgave.fristFerdigstillelse,
            aktivDato = oppgave.aktivDato,
            opprettetAv = oppgave.opprettetAv,
            endretAv = oppgave.endretAv,
            opprettetTidspunkt = oppgave.opprettetTidspunkt,
            endretTidspunkt = oppgave.endretTidspunkt,
            ferdigstiltTidspunkt = oppgave.ferdigstiltTidspunkt,
            behandlesAvApplikasjon = oppgave.behandlesAvApplikasjon,
            journalpostkilde = oppgave.journalpostkilde,
            ident = Ident(
                id = oppgave.ident?.id,
                identType = IdentType.valueOf(oppgave.ident?.identType?.name ?: ""),
                verdi = oppgave.ident?.verdi ?: throw RuntimeException("missing verdi"),
                folkeregisterident = oppgave.ident.folkeregisterident,
                registrertDato = null
            ),
            metadata = oppgave.metadata?.map { (k, v) ->
                MetadataNoekkel.valueOf(k.name) to v
            }?.toMap() ?: emptyMap()
        )
    }

    fun mapOppgaveApiRecordToOppgaveKopi(oppgave: OppgaveApiRecord): OppgaveKopi {
        return OppgaveKopi(
            id = oppgave.id,
            versjon = oppgave.versjon,
            journalpostId = oppgave.journalpostId,
            saksreferanse = oppgave.saksreferanse,
            mappeId = oppgave.mappeId,
            status = Status.valueOf(oppgave.status?.name ?: ""),
            tildeltEnhetsnr = oppgave.tildeltEnhetsnr ?: throw RuntimeException("missing tildeltEnhetsnr"),
            opprettetAvEnhetsnr = oppgave.opprettetAvEnhetsnr,
            endretAvEnhetsnr = oppgave.endretAvEnhetsnr,
            tema = oppgave.tema,
            temagruppe = oppgave.temagruppe,
            behandlingstema = oppgave.behandlingstema,
            oppgavetype = oppgave.oppgavetype ?: throw RuntimeException("missing oppgavetype"),
            behandlingstype = oppgave.behandlingstype,
            prioritet = Prioritet.valueOf(oppgave.prioritet?.name ?: ""),
            tilordnetRessurs = oppgave.tilordnetRessurs,
            beskrivelse = oppgave.beskrivelse,
            fristFerdigstillelse = oppgave.fristFerdigstillelse,
            aktivDato = LocalDate.parse(oppgave.aktivDato),
            opprettetAv = oppgave.opprettetAv ?: throw RuntimeException("missing opprettetAv"),
            endretAv = oppgave.endretAv,
            opprettetTidspunkt = oppgave.opprettetTidspunkt,
            endretTidspunkt = oppgave.endretTidspunkt,
            ferdigstiltTidspunkt = oppgave.ferdigstiltTidspunkt,
            behandlesAvApplikasjon = oppgave.behandlesAvApplikasjon,
            journalpostkilde = oppgave.journalpostkilde,
            ident = getIdent(oppgave.identer),
            metadata = oppgave.metadata?.map { (k, v) ->
                MetadataNoekkel.valueOf(k) to v
            }?.toMap() ?: emptyMap()
        )
    }

    fun bulkUpdateHjemmel(request: BatchUpdateRequest): BatchUpdateResponse {
        return oppgaveApiService.bulkUpdateHjemmel(request)
    }

    fun batchStore(request: BatchStoreRequest): BatchStoreResponse {

        val oppgaver = oppgaveApiService.fetchOppgaver(
            includeFrom = request.includeFrom,
            tema = request.tema,
            behandlingstype = request.behandlingstype,
            tildeltEnhetsnr = request.tildeltEnhetsnr
        )

        var countOK = 0

        if (!request.dryRun) {
            oppgaver.forEach { oppgave ->
                runCatching {
                    oppgaveKopiService.saveOppgaveKopi(mapOppgaveApiRecordToOppgaveKopi(oppgave))
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

    private fun getIdent(identer: List<no.nav.klage.oppgave.domain.Ident>?): Ident {
        if (identer != null) {
            val folkeregisterIdent = identer.find { it.gruppe == Gruppe.FOLKEREGISTERIDENT }
            val aktoerIdIdent = identer.find { it.gruppe == Gruppe.AKTOERID }
            return Ident(
                identType = IdentType.AKTOERID,
                verdi = aktoerIdIdent?.ident ?: "missing aktoerId",
                folkeregisterident = folkeregisterIdent?.ident
            )
        }

        throw RuntimeException("missing ident")
    }
}