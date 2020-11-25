package no.nav.klage.oppgave.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class OppgaveKafkaRecord(
    val id: Long,
    val tildeltEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val saksreferanse: String? = null,
    val tilordnetRessurs: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int? = null,
    val beskrivelse: String? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val aktivDato: LocalDate? = null,
    val opprettetTidspunkt: LocalDateTime? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    val ferdigstiltTidspunkt: LocalDateTime? = null,
    val endretTidspunkt: LocalDateTime? = null,
    val prioritet: Prioritet? = null,
    val status: Oppgavestatus? = null,
    val statuskategori: Oppgavestatuskategori? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val ident: Ident? = null,
    val mappeId: Long? = null,
    val metadata: Map<MetadataKey, String>
) {
    enum class Prioritet {
        HOY,
        NORM,
        LAV
    }

    enum class Oppgavestatus {
        OPPRETTET,
        AAPNET,
        UNDER_BEHANDLING,
        FERDIGSTILT,
        FEILREGISTRERT
    }

    enum class Oppgavestatuskategori {
        AAPEN, AVSLUTTET
    }

    data class Ident(
        val identType: IdentType,
        val verdi: String,
        val folkeregisterident: String,
        val registrert_dato: LocalDate
    )

    enum class IdentType {
        AKTOERID, ORGNR, SAMHANDLERNR, BNR
    }

    enum class MetadataKey {
        NORM_DATO, REVURDERINGSTYPE, SOKNAD_ID, KRAV_ID, MOTTATT_DATO, EKSTERN_HENVENDELSE_ID, SKANNET_DATO, RINA_SAKID, HJEMMEL
    }

}

data class Oppgave(
    val id: Long,
    val tildeltEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    val aktoerId: String? = null,
    val identer: List<Ident>? = null,
    val orgnr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: String,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int,
    val mappeId: Long? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    val prioritet: Prioritet? = null,
    val status: Status? = null,
    val metadata: Map<String, String>? = null,
    val fristFerdigstillelse: LocalDate?,
    val aktivDato: String? = null,
    val opprettetTidspunkt: String? = null,
    val ferdigstiltTidspunkt: String? = null,
    val endretTidspunkt: String? = null
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
    val oppgaver: List<Oppgave>
)
