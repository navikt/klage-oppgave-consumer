package no.nav.klage.oppgave.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate
import java.time.LocalDateTime

data class OppgaveApiRecord(
    val id: Long,
    val versjon: Int,
    val journalpostId: String? = null,
    val saksreferanse: String? = null,
    val mappeId: Long? = null,
    val status: Status? = null,
    val tildeltEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val tema: String,
    val temagruppe: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val prioritet: Prioritet? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val fristFerdigstillelse: LocalDate?,
    val aktivDato: String? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val endretTidspunkt: LocalDateTime? = null,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val ferdigstiltTidspunkt: LocalDateTime? = null,
    val behandlesAvApplikasjon: String? = null,
    val journalpostkilde: String? = null,
    val identer: List<Ident>? = null,
    val metadata: Map<String, String>? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    val aktoerId: String? = null,
    val orgnr: String? = null,
)

data class Ident(
    val ident: String? = null,
    val gruppe: Gruppe? = null
)

enum class Gruppe {
    FOLKEREGISTERIDENT, AKTOERID, NPID
}

enum class Prioritet {
    HOY, NORM, LAV
}

enum class Status {
    OPPRETTET, AAPNET, UNDER_BEHANDLING, FERDIGSTILT, FEILREGISTRERT
}

data class OppgaveResponse(
    val antallTreffTotalt: Int,
    val oppgaver: List<OppgaveApiRecord>
)

enum class Statuskategori {
    AAPEN, AVSLUTTET
}
