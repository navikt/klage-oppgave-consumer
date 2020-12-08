package no.nav.klage.oppgave.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

const val BEHANDLINGSTYPE_KLAGE = "ae0058"
const val KLAGEENHET_PREFIX = "42"

/**
 * Not reading all properties from record. There are some weird date formats
 * that we must handle later.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveKafkaRecord(
    val id: Long,
    val tildeltEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val tilordnetRessurs: String? = null,
    val tema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int? = null,
    val beskrivelse: String? = null,
    val metadata: Map<MetadataKey, String>?
) {

    fun isKlage() = behandlingstype == BEHANDLINGSTYPE_KLAGE
    fun isTildeltKlageenhet() = tildeltEnhetsnr?.startsWith(KLAGEENHET_PREFIX) == true

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

enum class Statuskategori {
    AAPEN, AVSLUTTET
}
