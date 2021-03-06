package no.nav.klage.oppgave.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate
import java.time.LocalDateTime

const val BEHANDLINGSTYPE_KLAGE = "ae0058"
const val KLAGEENHET_PREFIX = "42"

data class OppgaveKafkaRecord(
    val id: Long,
    val versjon: Int,
    val journalpostId: String? = null,
    val saksreferanse: String? = null,
    val mappeId: Long? = null,
    val status: Status,
    val statuskategori: Statuskategori? = null,
    val tildeltEnhetsnr: String,
    val opprettetAvEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val tema: String,
    val temagruppe: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String,
    val behandlingstype: String? = null,
    val prioritet: Prioritet,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val aktivDato: LocalDate,
    val opprettetAv: String,
    val endretAv: String? = null,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val endretTidspunkt: LocalDateTime? = null,
    @JsonDeserialize(using = OffsetDateTimeToLocalDateTimeDeserializer::class)
    val ferdigstiltTidspunkt: LocalDateTime? = null,
    val behandlesAvApplikasjon: String? = null,
    val journalpostkilde: String? = null,
    val ident: Ident? = null,
    val metadata: Map<MetadataKey, String>?
) {

    fun isKlage() = behandlingstype == BEHANDLINGSTYPE_KLAGE
    fun isAapen() = statuskategori == Statuskategori.AAPEN
    fun isTildeltKlageenhet() = tildeltEnhetsnr.startsWith(KLAGEENHET_PREFIX)

    enum class MetadataKey {
        NORM_DATO, REVURDERINGSTYPE, SOKNAD_ID, KRAV_ID, MOTTATT_DATO, EKSTERN_HENVENDELSE_ID, SKANNET_DATO, RINA_SAKID, HJEMMEL
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Ident(
        val id: Long? = null,
        val identType: IdentType,
        val verdi: String,
        val folkeregisterident: String? = null,
        //Not properly serialized. Ignore for now
//        val registrertDato: LocalDate? = null
    )

    enum class IdentType {
        AKTOERID, ORGNR, SAMHANDLERNR, BNR
    }

    enum class Status {
        OPPRETTET, AAPNET, UNDER_BEHANDLING, FERDIGSTILT, FEILREGISTRERT
    }

    enum class Statuskategori {
        AAPEN, AVSLUTTET
    }

    enum class Prioritet {
        HOY, NORM, LAV
    }
}