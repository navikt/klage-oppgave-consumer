package no.nav.klage.oppgave.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class OppgaveKafkaRecord(
        val id: Long,
        val tildeltEnhetsnr: String,
        val endretAvEnhetsnr: String,
        val opprettetAvEnhetsnr: String,
        val journalpostId: String,
        val saksreferanse: String,
        val tilordnetRessurs: String,
        val temagruppe: String,
        val tema: String,
        val behandlingstema: String,
        val oppgavetype: String,
        val behandlingstype: String,
        val versjon: Int,
        val beskrivelse: String,
        val fristFerdigstillelse: LocalDate,
        val aktivDato: LocalDate,
        val opprettetTidspunkt: LocalDateTime,
        val opprettetAv: String,
        val endretAv: String,
        val ferdigstiltTidspunkt: LocalDateTime,
        val endretTidspunkt: LocalDateTime,
        val prioritet: Prioritet,
        val status: Oppgavestatus,
        val statuskategori: Oppgavestatuskategori,
        val journalpostkilde: String,
        val behandlesAvApplikasjon: String,
        val ident: Ident,
        val mappeId: Long,
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